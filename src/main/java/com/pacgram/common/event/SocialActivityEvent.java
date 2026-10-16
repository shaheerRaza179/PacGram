package com.pacgram.common.event;

public record SocialActivityEvent(
        Long actorId,
        Long recipientId,
        NotificationType type,
        Long postId,
        String preview) {
}
