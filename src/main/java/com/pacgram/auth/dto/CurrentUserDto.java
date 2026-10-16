package com.pacgram.auth.dto;

public record CurrentUserDto(Long userId, String username, String name, String email) {
}
