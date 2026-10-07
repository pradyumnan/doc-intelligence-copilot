package com.pradyumnan.bpm_service.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pradyumnan.bpm_service.dto.DecisionResponse;
import com.pradyumnan.bpm_service.model.Case;
import com.pradyumnan.bpm_service.repository.CaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CaseService {

    private static final Logger logger = LoggerFactory.getLogger(CaseService.class);

    private final RestClient restClient;
    private final CaseRepository caseRepository;

    public CaseService(CaseRepository caseRepository,
                        @Value("${ai.service.url}") String aiServiceUrl) {
        this.caseRepository = caseRepository;
        this.restClient = RestClient.builder().baseUrl(aiServiceUrl).build();
    }

    // Inner class to match Python's snake_case JSON field names
    static class AiDecisionRequest {
        @JsonProperty("document_text")
        public String documentText;
        @JsonProperty("filename")
        public String filename;

        public AiDecisionRequest(String documentText, String filename) {
            this.documentText = documentText;
            this.filename = filename;
        }
    }

    private Case buildAndSaveCase(DecisionResponse aiResponse) {
        logger.info("Decision for {}: category={}, route={}, confidence={}, status={}",
                aiResponse.getFilename(), aiResponse.getCategory(), aiResponse.getRoute(),
                aiResponse.getConfidence(), aiResponse.getFinalStatus());

        Case documentCase = new Case();
        documentCase.setFilename(aiResponse.getFilename());
        documentCase.setCategory(aiResponse.getCategory());
        documentCase.setRoute(aiResponse.getRoute());
        documentCase.setJustification(aiResponse.getJustification());
        documentCase.setConfidence(aiResponse.getConfidence());
        documentCase.setFinalStatus(aiResponse.getFinalStatus());

        Case saved = caseRepository.save(documentCase);
        logger.info("Saved case id={} for {}", saved.getId(), aiResponse.getFilename());
        return saved;
    }

    public Case processDocument(String documentText, String filename) {
        logger.info("Processing text document: {}", filename);

        AiDecisionRequest request = new AiDecisionRequest(documentText, filename);

        DecisionResponse aiResponse;
        try {
            aiResponse = restClient.post()
                    .uri("/agent/decide")
                    .body(request)
                    .retrieve()
                    .body(DecisionResponse.class);
        } catch (Exception e) {
            logger.error("AI service call failed for {}: {}", filename, e.getMessage());
            throw e;
        }

        return buildAndSaveCase(aiResponse);
    }

    public Case processDocumentImage(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        logger.info("Processing image document: {}", filename);

        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        DecisionResponse aiResponse;
        try {
            aiResponse = restClient.post()
                    .uri("/agent/decide-from-image")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(DecisionResponse.class);
        } catch (Exception e) {
            logger.error("AI service call failed for image {}: {}", filename, e.getMessage());
            throw e;
        }

        return buildAndSaveCase(aiResponse);
    }

    public java.util.List<Case> getAllCases() {
        return caseRepository.findAll();
    }
}