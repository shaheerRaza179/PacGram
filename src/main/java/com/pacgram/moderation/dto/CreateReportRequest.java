package com.pacgram.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotNull Long reportedUserId,
        @NotBlank @Size(max = 60) String reason,
        @NotBlank @Size(min = 10, max = 1000) String details,
        @Size(max = 30) String contentType,
        Long contentId) {
}
