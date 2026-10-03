package com.pradyumnan.bpm_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DecisionResponse {
    private String filename;
    private String category;
    private String route;
    private String justification;
    private Double confidence;

    @JsonProperty("final_status")
    private String finalStatus;

    // Getters and setters
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getFinalStatus() { return finalStatus; }
    public void setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; }
}