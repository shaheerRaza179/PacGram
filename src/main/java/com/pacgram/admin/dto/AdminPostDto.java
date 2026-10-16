package com.pacgram.admin.dto;

import java.time.Instant;

public record AdminPostDto(
        Long id,
        String caption,
        String imageUrl,
        Instant createdAt,
        long likesCount,
        long commentsCount) {
}
