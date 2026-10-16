package com.pacgram.user.dto;

import java.time.Instant;

public record UserProfileDto(
        Long id,
        String name,
        String username,
        String email,
        String avatarUrl,
        String coverUrl,
        String bio,
        boolean privateAccount,
        boolean followedByViewer,
        long followersCount,
        long followingCount,
        long postsCount,
        Instant createdAt) {
}
