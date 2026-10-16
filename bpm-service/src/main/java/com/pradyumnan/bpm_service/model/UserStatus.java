package com.pradyumnan.bpm_service.model;

public enum UserStatus {
    PENDING,   // self-registered, waiting for an administrator to approve
    ACTIVE,    // can sign in
    DISABLED   // blocked by an administrator
}
