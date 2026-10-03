package com.pradyumnan.bpm_service.dto;

public class DecisionRequest {
    private String documentText;
    private String filename;

    public DecisionRequest(String documentText, String filename) {
        this.documentText = documentText;
        this.filename = filename;
    }

    public String getDocumentText() { return documentText; }
    public void setDocumentText(String documentText) { this.documentText = documentText; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
}