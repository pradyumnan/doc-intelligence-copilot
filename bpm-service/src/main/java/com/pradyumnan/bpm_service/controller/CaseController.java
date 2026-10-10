package com.pradyumnan.bpm_service.controller;

import com.pradyumnan.bpm_service.model.Case;
import com.pradyumnan.bpm_service.service.CaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
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

    public static class ProcessRequest {
        public String documentText;
        public String filename;
    }

    @GetMapping
    public List<Case> getAllCases() {
        return caseService.getAllCases();
    }

    @PostMapping("/process")
    public Case process(@RequestBody ProcessRequest request) {
        return caseService.processDocument(request.documentText, request.filename);
    }

    @PostMapping(value = "/process-image", consumes = "multipart/form-data")
    public ResponseEntity<?> processImage(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only PNG or JPEG images are supported"));
        }
        if (file.getSize() > MAX_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "File too large (max 10 MB)"));
        }
        return ResponseEntity.ok(caseService.processDocumentImage(file));
    }

    // If ai-service rejects the request (e.g. a fake image), return a clean 400 instead of a 500
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<?> handleAiClientError(HttpClientErrorException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "The uploaded file could not be processed as a valid PNG/JPEG image"));
    }
}