package com.pradyumnan.bpm_service.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pradyumnan.bpm_service.dto.DecisionResponse;
import com.pradyumnan.bpm_service.model.Case;
import com.pradyumnan.bpm_service.repository.CaseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CaseService {

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

    public Case processDocument(String documentText, String filename) {
        AiDecisionRequest request = new AiDecisionRequest(documentText, filename);

        DecisionResponse aiResponse = restClient.post()
                .uri("/agent/decide")
                .body(request)
                .retrieve()
                .body(DecisionResponse.class);

        Case documentCase = new Case();
        documentCase.setFilename(aiResponse.getFilename());
        documentCase.setCategory(aiResponse.getCategory());
        documentCase.setRoute(aiResponse.getRoute());
        documentCase.setJustification(aiResponse.getJustification());
        documentCase.setConfidence(aiResponse.getConfidence());
        documentCase.setFinalStatus(aiResponse.getFinalStatus());

        return caseRepository.save(documentCase);
    }
}