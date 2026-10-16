package com.pacgram.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateReportRequest(
        @NotBlank @Pattern(regexp = "PENDING|IN_REVIEW|RESOLVED|DISMISSED") String status,
        @Size(max = 1000) String resolutionNote) {
}
