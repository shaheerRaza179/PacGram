package com.pacgram.post.controller;

import com.pacgram.common.api.ApiResponse;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.post.dto.AddCommentRequest;
import com.pacgram.post.dto.CommentDto;
import com.pacgram.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@Tag(name = "Comments")
@SecurityRequirement(name = "bearerAuth")
public class CommentController {
    private final PostService postService;

    public CommentController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a comment to a post")
    public ApiResponse<CommentDto> add(
            @PathVariable Long postId,
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody AddCommentRequest request) {
        return ApiResponse.success(postService.addComment(postId, principal, request));
    }

    @GetMapping
    @Operation(summary = "List post comments")
    public ApiResponse<Page<CommentDto>> list(
            @PathVariable Long postId,
            @AuthenticationPrincipal AccountPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(postService.comments(postId, principal, pageable));
    }
}
