package com.pradyumnan.bpm_service.controller;

import com.pradyumnan.bpm_service.dto.CaseDetail;
import com.pradyumnan.bpm_service.dto.CaseSummary;
import com.pradyumnan.bpm_service.dto.ReviewRequest;
import com.pradyumnan.bpm_service.model.CaseStatus;
import com.pradyumnan.bpm_service.service.ApiException;
import com.pradyumnan.bpm_service.service.CaseService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/cases")
public class CaseController {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg");

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    // ---- reading: every signed-in role (see SecurityConfig)

    @GetMapping
    public List<CaseSummary> list(@RequestParam(required = false) String queue,
                                  @RequestParam(required = false) CaseStatus status) {
        return caseService.list(queue, status);
    }

    @GetMapping("/{id}")
    public CaseDetail get(@PathVariable Long id) {
        return caseService.getDetail(id);
    }

    // The document itself, only ever served through this authenticated endpoint
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable Long id) throws IOException {
        CaseService.StoredFile file = caseService.loadFile(id);
        MediaType type = file.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(file.contentType());
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(file.bytes());
    }

    // ---- working with documents: reviewers and admins (see SecurityConfig)

    @PostMapping(value = "/process-image", consumes = "multipart/form-data")
    public ResponseEntity<?> processImage(@RequestParam("file") MultipartFile file, Authentication auth)
            throws IOException {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PNG or JPEG images are supported");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File too large (max 10 MB)");
        }
        return ResponseEntity.ok(CaseSummary.from(caseService.processDocumentImage(file, auth.getName())));
    }

    @PostMapping("/{id}/approve")
    public CaseDetail approve(@PathVariable Long id, @RequestBody(required = false) ReviewRequest request,
                              Authentication auth) {
        caseService.approve(id, auth.getName(), request == null ? null : request.note());
        return caseService.getDetail(id);
    }

    @PostMapping("/{id}/reassign")
    public CaseDetail reassign(@PathVariable Long id, @RequestBody ReviewRequest request, Authentication auth) {
        caseService.reassign(id, auth.getName(), request == null ? null : request.queue(),
                request == null ? null : request.note());
        return caseService.getDetail(id);
    }

    @PostMapping("/{id}/reject")
    public CaseDetail reject(@PathVariable Long id, @RequestBody(required = false) ReviewRequest request,
                             Authentication auth) {
        caseService.reject(id, auth.getName(), request == null ? null : request.note());
        return caseService.getDetail(id);
    }

    @PostMapping("/{id}/complete")
    public CaseDetail complete(@PathVariable Long id, @RequestBody(required = false) ReviewRequest request,
                               Authentication auth) {
        caseService.complete(id, auth.getName(), request == null ? null : request.note());
        return caseService.getDetail(id);
    }
}
