package com.pacgram.admin.dto;

import java.time.Instant;

public record AdminBlockDto(
        Long id,
        Long blockerId,
        String blockerUsername,
        Long blockedId,
        String blockedUsername,
        Instant createdAt) {
}
