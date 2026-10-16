package com.pacgram.messaging.dto;

import com.pacgram.user.dto.UserSummaryDto;

import java.time.Instant;

public record ConversationDto(
        Long id,
        UserSummaryDto participant,
        MessageDto lastMessage,
        long unreadCount,
        Instant lastMessageAt) {
}
