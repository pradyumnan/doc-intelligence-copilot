package com.pradyumnan.bpm_service.controller;

import com.pradyumnan.bpm_service.model.Case;
import com.pradyumnan.bpm_service.service.CaseService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    public static class ProcessRequest {
        public String documentText;
        public String filename;
    }

    @PostMapping("/process")
    public Case process(@RequestBody ProcessRequest request) {
        return caseService.processDocument(request.documentText, request.filename);
    }

    @PostMapping(value = "/process-image", consumes = "multipart/form-data")
public Case processImage(@RequestParam("file") MultipartFile file) throws Exception {
    return caseService.processDocumentImage(file);
}
}