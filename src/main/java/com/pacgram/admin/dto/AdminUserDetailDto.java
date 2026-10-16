package com.pacgram.admin.dto;

public record AdminUserDetailDto(
        AdminUserDto account,
        String bio,
        String avatarUrl,
        String coverUrl,
        boolean privateAccount,
        boolean activityStatus,
        long followersCount,
        long followingCount,
        long reportsMadeCount,
        long reportsReceivedCount,
        long blockedByCount,
        long blockingCount) {
}
