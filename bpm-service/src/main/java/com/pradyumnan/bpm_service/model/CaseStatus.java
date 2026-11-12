package com.pradyumnan.bpm_service.model;

public enum CaseStatus {
    IN_QUEUE,        // routed to a work queue, waiting for that team
    PENDING_REVIEW,  // held for a person to check and route
    COMPLETED,       // the queue's work is done
    REJECTED         // a reviewer rejected the document
}
