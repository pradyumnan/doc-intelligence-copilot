package com.pradyumnan.bpm_service.dto;

import com.pradyumnan.bpm_service.model.Case;
import com.pradyumnan.bpm_service.model.CaseStatus;

import java.time.LocalDateTime;

/** What the case list needs. Deliberately leaves out the large OCR text. */
public record CaseSummary(
        Long id,
        String filename,
        String category,
        String route,
        String queue,
        CaseStatus status,
        Double confidence,
        String finalStatus,
        String justification,
        String uploadedBy,
        String routedBy,
        LocalDateTime createdAt) {

    public static CaseSummary from(Case c) {
        return new CaseSummary(c.getId(), c.getFilename(), c.getCategory(), c.getRoute(), c.getQueue(),
                c.getStatus(), c.getConfidence(), c.getFinalStatus(), c.getJustification(),
                c.getUploadedBy(), c.getRoutedBy(), c.getCreatedAt());
    }
}
