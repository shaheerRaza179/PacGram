package com.pacgram.user.dto;

public record UserSummaryDto(
        Long id,
        String name,
        String username,
        String avatarUrl,
        String bio,
        boolean followedByViewer) {
}
