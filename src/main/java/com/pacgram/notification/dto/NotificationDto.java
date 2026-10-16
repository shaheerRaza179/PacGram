package com.pacgram.notification.dto;

import com.pacgram.common.event.NotificationType;
import com.pacgram.user.dto.UserSummaryDto;

import java.time.Instant;

public record NotificationDto(
        Long id,
        NotificationType type,
        UserSummaryDto actor,
        Long postId,
        String preview,
        boolean read,
        Instant createdAt) {
}
