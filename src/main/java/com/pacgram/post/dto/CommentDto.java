package com.pacgram.post.dto;

import com.pacgram.user.dto.UserSummaryDto;

import java.time.Instant;

public record CommentDto(Long id, UserSummaryDto author, String text, Instant createdAt) {
}
