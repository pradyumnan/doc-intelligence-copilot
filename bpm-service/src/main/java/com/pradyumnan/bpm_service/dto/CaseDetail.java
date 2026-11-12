package com.pradyumnan.bpm_service.dto;

import com.pradyumnan.bpm_service.model.CaseStatus;

import java.time.LocalDateTime;
import java.util.List;

/** One case in full, including what the OCR read and the audit trail. */
public record CaseDetail(
        Long id,
        String filename,
        String category,
        String route,
        String queue,
        CaseStatus status,
        Double confidence,
        String finalStatus,
        String justification,
        Boolean verified,
        String policySource,
        String uploadedBy,
        String routedBy,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewNote,
        LocalDateTime createdAt,
        boolean hasFile,
        String extractedText,
        String fieldsJson,
        List<EventView> events) {}
