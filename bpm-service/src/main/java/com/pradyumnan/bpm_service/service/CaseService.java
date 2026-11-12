package com.pradyumnan.bpm_service.service;

import com.pradyumnan.bpm_service.dto.CaseDetail;
import com.pradyumnan.bpm_service.dto.CaseSummary;
import com.pradyumnan.bpm_service.dto.DecisionResponse;
import com.pradyumnan.bpm_service.dto.EventView;
import com.pradyumnan.bpm_service.model.Case;
import com.pradyumnan.bpm_service.model.CaseEvent;
import com.pradyumnan.bpm_service.model.CaseStatus;
import com.pradyumnan.bpm_service.repository.CaseEventRepository;
import com.pradyumnan.bpm_service.repository.CaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CaseService {

    public record QueueView(String name, long count, boolean system) {}

    public record StoredFile(byte[] bytes, String contentType) {}

    private static final Logger logger = LoggerFactory.getLogger(CaseService.class);

    private final RestClient restClient;
    private final CaseRepository caseRepository;
    private final CaseEventRepository eventRepository;
    private final FileStorage storage;

    public CaseService(CaseRepository caseRepository,
                       CaseEventRepository eventRepository,
                       FileStorage storage,
                       @Value("${ai.service.url}") String aiServiceUrl) {
        this.caseRepository = caseRepository;
        this.eventRepository = eventRepository;
        this.storage = storage;
        this.restClient = RestClient.builder().baseUrl(aiServiceUrl).build();
    }

    // ---------------------------------------------------------------- upload

    public Case processDocumentImage(MultipartFile file, String uploader) throws IOException {
        String filename = file.getOriginalFilename();
        byte[] bytes = file.getBytes();
        logger.info("Processing image document {} uploaded by {}", filename, uploader);

        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        // Failures here are turned into clean error responses by ApiExceptionHandler
        DecisionResponse ai = restClient.post()
                .uri("/agent/decide-from-image")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(DecisionResponse.class);
        if (ai == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The analysis service returned no result.");
        }

        // Keep the file only once the analysis has succeeded
        String extension = "image/png".equals(file.getContentType()) ? "png" : "jpg";
        String key = storage.save(bytes, extension);

        Case c = new Case();
        c.setFilename(filename);
        c.setCategory(ai.getCategory());
        c.setRoute(ai.getRoute());
        c.setJustification(limit(ai.getJustification(), 1000));
        c.setConfidence(ai.getConfidence());
        c.setFinalStatus(ai.getFinalStatus());
        c.setVerified(ai.getVerified());
        c.setPolicySource(ai.getPolicySource());
        c.setExtractedText(ai.getExtractedText());
        c.setFieldsJson(ai.getFieldsJson());
        c.setUploadedBy(uploader);
        c.setStorageKey(key);
        c.setContentType(file.getContentType());
        c.setFileHash(sha256(bytes));

        // Only a confident, verified decision to a real queue is routed automatically.
        // Anything else, including a queue name we do not recognise, waits for a person.
        Optional<String> target = Queues.find(ai.getRoute());
        boolean autoRouted = "auto_routed".equals(ai.getFinalStatus()) && target.isPresent();
        if (autoRouted) {
            c.setStatus(CaseStatus.IN_QUEUE);
            c.setQueue(target.get());
            c.setRoutedBy("AI");
        } else {
            c.setStatus(CaseStatus.PENDING_REVIEW);
            c.setQueue(Queues.HUMAN_REVIEW);
        }

        Case saved = caseRepository.save(c);
        record(saved.getId(), uploader, "UPLOADED", null, null, null);
        if (autoRouted) {
            record(saved.getId(), "AI", "AI_ROUTED", null, saved.getQueue(), null);
        } else {
            record(saved.getId(), "AI", "AI_HELD", null, Queues.HUMAN_REVIEW, limit(ai.getJustification(), 300));
        }
        logger.info("Saved case id={} status={} queue={}", saved.getId(), saved.getStatus(), saved.getQueue());
        return saved;
    }

    // ----------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public List<CaseSummary> list(String queue, CaseStatus status) {
        return caseRepository.findAllSummaries().stream()
                .filter(c -> queue == null || queue.equals(c.queue()))
                .filter(c -> status == null || status == c.status())
                .toList();
    }

    @Transactional(readOnly = true)
    public CaseDetail getDetail(Long id) {
        Case c = require(id);
        List<EventView> events = eventRepository.findByCaseIdOrderByOccurredAtAscIdAsc(id).stream()
                .map(e -> new EventView(e.getId(), e.getOccurredAt(), e.getActor(), e.getAction(),
                        e.getFromQueue(), e.getToQueue(), e.getNote()))
                .toList();
        return new CaseDetail(c.getId(), c.getFilename(), c.getCategory(), c.getRoute(), c.getQueue(),
                c.getStatus(), c.getConfidence(), c.getFinalStatus(), c.getJustification(), c.getVerified(),
                c.getPolicySource(), c.getUploadedBy(), c.getRoutedBy(), c.getReviewedBy(), c.getReviewedAt(),
                c.getReviewNote(), c.getCreatedAt(), c.getStorageKey() != null, c.getExtractedText(),
                c.getFieldsJson(), events);
    }

    @Transactional(readOnly = true)
    public StoredFile loadFile(Long id) throws IOException {
        Case c = require(id);
        if (c.getStorageKey() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No document is stored for this case.");
        }
        try {
            return new StoredFile(storage.load(c.getStorageKey()), c.getContentType());
        } catch (NoSuchFileException e) {
            logger.warn("File for case {} is missing from storage", id);
            throw new ApiException(HttpStatus.NOT_FOUND, "The stored document could not be found.");
        }
    }

    @Transactional(readOnly = true)
    public List<QueueView> queueOverview() {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : caseRepository.countByQueue(CaseStatus.IN_QUEUE)) {
            counts.put((String) row[0], (Long) row[1]);
        }
        List<QueueView> result = new ArrayList<>();
        result.add(new QueueView(Queues.HUMAN_REVIEW, caseRepository.countByStatus(CaseStatus.PENDING_REVIEW), true));
        for (String queue : Queues.WORK_QUEUES) {
            result.add(new QueueView(queue, counts.getOrDefault(queue, 0L), false));
        }
        return result;
    }

    // --------------------------------------------------------- review actions

    /** Accept the AI's suggested queue for a case it held. */
    @Transactional
    public Case approve(Long id, String reviewer, String note) {
        Case c = require(id);
        if (c.getStatus() != CaseStatus.PENDING_REVIEW) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a case waiting for review can be approved.");
        }
        String from = c.getQueue();
        String target = Queues.find(c.getRoute()).orElse(Queues.GENERAL_REVIEW);
        moveTo(c, target, reviewer, note);
        record(id, reviewer, "APPROVED", from, target, note);
        return c;
    }

    /** Send a case to a different queue: either a held case, or a wrong automatic routing. */
    @Transactional
    public Case reassign(Long id, String reviewer, String queue, String note) {
        Case c = require(id);
        if (c.getStatus() != CaseStatus.PENDING_REVIEW && c.getStatus() != CaseStatus.IN_QUEUE) {
            throw new ApiException(HttpStatus.CONFLICT, "This case is already closed.");
        }
        String target = Queues.find(queue)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Choose one of the listed queues."));
        if (target.equals(c.getQueue())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The case is already in that queue.");
        }
        String from = c.getQueue();
        moveTo(c, target, reviewer, note);
        record(id, reviewer, "REASSIGNED", from, target, note);
        return c;
    }

    @Transactional
    public Case reject(Long id, String reviewer, String note) {
        Case c = require(id);
        if (c.getStatus() != CaseStatus.PENDING_REVIEW && c.getStatus() != CaseStatus.IN_QUEUE) {
            throw new ApiException(HttpStatus.CONFLICT, "This case is already closed.");
        }
        if (note == null || note.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A reason is required to reject a document.");
        }
        String from = c.getQueue();
        c.setStatus(CaseStatus.REJECTED);
        stamp(c, reviewer, note);
        caseRepository.save(c);
        record(id, reviewer, "REJECTED", from, null, note);
        return c;
    }

    /** The queue's team has finished with the document. */
    @Transactional
    public Case complete(Long id, String actor, String note) {
        Case c = require(id);
        if (c.getStatus() != CaseStatus.IN_QUEUE) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a case waiting in a queue can be completed.");
        }
        c.setStatus(CaseStatus.COMPLETED);
        caseRepository.save(c);
        record(id, actor, "COMPLETED", c.getQueue(), null, note);
        return c;
    }

    // --------------------------------------------------------------- helpers

    private void moveTo(Case c, String queue, String reviewer, String note) {
        c.setQueue(queue);
        c.setStatus(CaseStatus.IN_QUEUE);
        c.setRoutedBy(reviewer);
        stamp(c, reviewer, note);
        caseRepository.save(c);
    }

    private void stamp(Case c, String reviewer, String note) {
        c.setReviewedBy(reviewer);
        c.setReviewedAt(LocalDateTime.now());
        c.setReviewNote(limit(note == null || note.isBlank() ? null : note.trim(), 500));
    }

    private void record(Long caseId, String actor, String action, String from, String to, String note) {
        eventRepository.save(new CaseEvent(caseId, actor, action, from, to,
                limit(note == null || note.isBlank() ? null : note.trim(), 500)));
    }

    private Case require(Long id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Case not found."));
    }

    private static String limit(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 is required to exist on every Java platform
        }
    }
}
