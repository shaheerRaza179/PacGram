package com.pacgram.post.dto;

import com.pacgram.user.dto.UserSummaryDto;

import java.time.Instant;

public record PostDto(
        Long id,
        UserSummaryDto author,
        String caption,
        String imageUrl,
        long likesCount,
        long commentsCount,
        boolean likedByViewer,
        boolean savedByViewer,
        Instant createdAt) {
}
