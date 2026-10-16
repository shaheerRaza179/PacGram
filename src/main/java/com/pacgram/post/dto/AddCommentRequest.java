package com.pacgram.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(@NotBlank @Size(max = 1000) String text) {
}
