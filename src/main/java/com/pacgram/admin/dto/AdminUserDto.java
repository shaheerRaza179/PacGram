package com.pacgram.admin.dto;

import java.time.Instant;

public record AdminUserDto(
        Long id,
        String name,
        String username,
        String email,
        String status,
        Instant lastSeenAt,
        Instant joinedAt,
        long postsCount) {
}
