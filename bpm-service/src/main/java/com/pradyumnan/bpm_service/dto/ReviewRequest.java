package com.pradyumnan.bpm_service.dto;

/** Body for review actions. "queue" is only used when reassigning; "note" is optional except for reject. */
public record ReviewRequest(String queue, String note) {}
