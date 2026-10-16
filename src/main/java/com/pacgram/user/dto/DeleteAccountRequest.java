package com.pacgram.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(@NotBlank @Size(max = 72) String password) {
}
