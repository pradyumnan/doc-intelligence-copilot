package com.pradyumnan.bpm_service.dto;

import java.time.LocalDateTime;

public record EventView(
        Long id,
        LocalDateTime occurredAt,
        String actor,
        String action,
        String fromQueue,
        String toQueue,
        String note) {}
