package com.pradyumnan.bpm_service.model;

public enum Role {
    ADMIN,     // manages users, can also do everything a reviewer can
    REVIEWER,  // uploads documents and works the review queues
    VIEWER     // read-only access to cases
}
