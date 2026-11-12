package com.pradyumnan.bpm_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** What ai-service returns for one document. Field names match its snake_case JSON. */
public class DecisionResponse {
    private String filename;
    private String category;
    private String route;
    private String justification;
    private Double confidence;

    @JsonProperty("final_status")
    private String finalStatus;

    private Boolean verified;

    @JsonProperty("policy_source")
    private String policySource;

    @JsonProperty("extracted_text")
    private String extractedText;

    // Dates, amounts, people and organisations, already serialised as a JSON string by ai-service
    @JsonProperty("fields_json")
    private String fieldsJson;

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

    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }

    public String getPolicySource() { return policySource; }
    public void setPolicySource(String policySource) { this.policySource = policySource; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getFieldsJson() { return fieldsJson; }
    public void setFieldsJson(String fieldsJson) { this.fieldsJson = fieldsJson; }
}
