package com.pacgram.messaging.dto;

public record TypingEvent(Long conversationId, Long userId, String username, boolean typing) {
}
