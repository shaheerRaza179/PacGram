package com.pacgram.user.dto;

import jakarta.validation.constraints.NotNull;

public record AccountSettingsRequest(
        @NotNull Boolean privateAccount,
        @NotNull Boolean activityStatus,
        @NotNull Boolean notifyLikes,
        @NotNull Boolean notifyComments,
        @NotNull Boolean notifyFollows,
        @NotNull Boolean notifyMessages) {
}
