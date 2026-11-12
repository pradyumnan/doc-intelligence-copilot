package com.pradyumnan.bpm_service.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** One line of a case's audit trail: who did what, and when. Never edited or deleted. */
@Entity
@Table(name = "case_events")
public class CaseEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long caseId;

    private LocalDateTime occurredAt;
    private String actor;      // a username, or "AI"
    private String action;     // UPLOADED, AI_ROUTED, AI_HELD, APPROVED, REASSIGNED, REJECTED, COMPLETED
    private String fromQueue;
    private String toQueue;
    @Column(length = 500)
    private String note;

    @PrePersist
    protected void onCreate() {
        occurredAt = LocalDateTime.now();
    }

    public CaseEvent() {}

    public CaseEvent(Long caseId, String actor, String action, String fromQueue, String toQueue, String note) {
        this.caseId = caseId;
        this.actor = actor;
        this.action = action;
        this.fromQueue = fromQueue;
        this.toQueue = toQueue;
        this.note = note;
    }

    public Long getId() { return id; }
    public Long getCaseId() { return caseId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getFromQueue() { return fromQueue; }
    public String getToQueue() { return toQueue; }
    public String getNote() { return note; }
}
