package com.pacgram.messaging.event;

import com.pacgram.messaging.dto.MessageDto;

public record MessageCreatedEvent(Long conversationId, MessageDto message) {
}
