package com.pradyumnan.bpm_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cases")
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String category;
    private String route;

    @Column(length = 1000)
    private String justification;

    private Double confidence;
    private String finalStatus;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}