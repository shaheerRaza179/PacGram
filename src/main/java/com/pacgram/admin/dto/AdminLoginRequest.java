package com.pacgram.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @NotBlank @Size(max = 40) String username,
        @NotBlank @Size(max = 72) String password) {
}
