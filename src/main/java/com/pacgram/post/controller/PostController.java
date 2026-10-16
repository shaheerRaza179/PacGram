package com.pacgram.post.controller;

import com.pacgram.common.api.ApiResponse;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.post.dto.CreatePostRequest;
import com.pacgram.post.dto.PostDto;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Posts")
@SecurityRequirement(name = "bearerAuth")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a post with an optional image")
    public ApiResponse<PostDto> create(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @ModelAttribute CreatePostRequest request) {
        return ApiResponse.success(postService.create(principal, request));
    }

    @GetMapping("/feed")
    @Operation(summary = "Get posts from followed accounts and the current user")
    public ApiResponse<Page<PostDto>> feed(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(postService.feed(principal, pageable));
    }

    @GetMapping("/explore")
    @Operation(summary = "Discover recent posts from accounts not followed")
    public ApiResponse<Page<PostDto>> explore(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(postService.explore(principal, pageable));
    }

    @GetMapping("/saved")
    @Operation(summary = "List the authenticated user's saved posts")
    public ApiResponse<Page<PostDto>> saved(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(postService.saved(principal, pageable));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List a user's posts")
    public ApiResponse<Page<PostDto>> byUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal AccountPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(postService.byUser(userId, principal, pageable));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Get one post")
    public ApiResponse<PostDto> get(
            @PathVariable Long postId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        return ApiResponse.success(postService.get(postId, principal));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete an owned post")
    public ApiResponse<String> delete(
            @PathVariable Long postId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        postService.delete(postId, principal);
        return ApiResponse.success("Post deleted.");
    }

    @PostMapping("/{postId}/like")
    @Operation(summary = "Like a post")
    public ApiResponse<String> like(
            @PathVariable Long postId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        postService.like(postId, principal);
        return ApiResponse.success("Post liked.");
    }

    @DeleteMapping("/{postId}/like")
    @Operation(summary = "Remove a like")
    public ApiResponse<String> unlike(
            @PathVariable Long postId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        postService.unlike(postId, principal);
        return ApiResponse.success("Like removed.");
    }

    @PostMapping("/{postId}/save")
    @Operation(summary = "Save a post to bookmarks")
    public ApiResponse<String> save(
            @PathVariable Long postId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        postService.save(postId, principal);
        return ApiResponse.success("Post saved.");
    }

    @DeleteMapping("/{postId}/save")
    @Operation(summary = "Remove a saved post")
    public ApiResponse<String> unsave(
            @PathVariable Long postId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        postService.unsave(postId, principal);
        return ApiResponse.success("Post removed from saved posts.");
    }
}
