package com.pacgram.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserStatusRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|SUSPENDED") String status) {
}
