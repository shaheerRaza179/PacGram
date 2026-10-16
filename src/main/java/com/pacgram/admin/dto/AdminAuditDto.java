package com.pacgram.admin.dto;

import java.time.Instant;

public record AdminAuditDto(
        Long id,
        String adminUsername,
        String action,
        String targetType,
        Long targetId,
        String description,
        Instant createdAt) {
}
