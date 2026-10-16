package com.pacgram.messaging.dto;

import java.time.Instant;

public record MessageDto(
        Long id,
        Long senderId,
        String senderUsername,
        String text,
        Instant createdAt,
        boolean read) {
}
