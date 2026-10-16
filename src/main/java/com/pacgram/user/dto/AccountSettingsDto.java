package com.pacgram.user.dto;

public record AccountSettingsDto(
        boolean privateAccount,
        boolean activityStatus,
        boolean notifyLikes,
        boolean notifyComments,
        boolean notifyFollows,
        boolean notifyMessages) {
}
