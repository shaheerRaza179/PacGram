package com.pacgram.admin.dto;

public record AdminDashboardDto(
        long totalUsers,
        long onlineUsers,
        long offlineUsers,
        long suspendedUsers,
        long reportedAccounts,
        long totalBlocks,
        long pendingReports,
        long inReviewReports,
        long resolvedReports,
        long dismissedReports,
        long totalReports,
        long newUsersThisWeek) {
}
