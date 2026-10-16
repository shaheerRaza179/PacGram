package com.pacgram.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 254) @JsonAlias({"email", "username"}) String identifier,
        @NotBlank @Size(max = 72) String password) {
}
