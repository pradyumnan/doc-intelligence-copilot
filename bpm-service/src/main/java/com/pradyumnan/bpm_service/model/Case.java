package com.pradyumnan.bpm_service.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Named CaseRecord for JPQL, because "Case" is also a keyword in HQL. The table is still "cases".
@Entity(name = "CaseRecord")
@Table(name = "cases")
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optimistic locking: two reviewers cannot silently overwrite each other
    @Version
    private Long version;

    private String filename;

    // What the AI decided (kept as the record of its decision, never edited by reviewers)
    private String category;
    private String route;
    @Column(length = 1000)
    private String justification;
    private Double confidence;
    private String finalStatus; // the AI gate's own verdict: auto_routed or needs_human_review
    private Boolean verified;
    private String policySource;

    // Where the case is now, and how it got there
    private String queue;
    @Enumerated(EnumType.STRING)
    private CaseStatus status;
    private String routedBy; // "AI", or the reviewer who routed it

    // The stored document
    private String uploadedBy;
    private String storageKey;
    private String contentType;
    private String fileHash; // SHA-256, kept so duplicates can be detected

    @Column(columnDefinition = "text")
    private String extractedText;
    @Column(columnDefinition = "text")
    private String fieldsJson;

    // Human review
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    @Column(length = 500)
    private String reviewNote;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

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

    public String getQueue() { return queue; }
    public void setQueue(String queue) { this.queue = queue; }

    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }

    public String getRoutedBy() { return routedBy; }
    public void setRoutedBy(String routedBy) { this.routedBy = routedBy; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getFieldsJson() { return fieldsJson; }
    public void setFieldsJson(String fieldsJson) { this.fieldsJson = fieldsJson; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
