package com.pacgram.post.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record CreatePostRequest(
        @Size(max = 2200) String caption,
        MultipartFile image) {

    @AssertTrue(message = "A post must include an image or a caption")
        public boolean isValid() {
        return (caption != null && !caption.isBlank()) || (image != null && !image.isEmpty());
    }
}
