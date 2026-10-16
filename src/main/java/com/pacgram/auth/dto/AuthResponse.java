package com.pacgram.auth.dto;

public record AuthResponse(
        String token,
        Long userId,
        String username,
        String name,
        String email) {
}
