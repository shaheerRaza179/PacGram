package com.pacgram.user.controller;

import com.pacgram.common.api.ApiResponse;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.user.dto.UserSummaryDto;
import com.pacgram.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Follows")
@SecurityRequirement(name = "bearerAuth")
public class FollowController {
    private final UserProfileService userService;

    public FollowController(UserProfileService userService) {
        this.userService = userService;
    }

    @PostMapping("/{id}/follow")
    @Operation(summary = "Follow a user")
    public ApiResponse<String> follow(
            @PathVariable Long id, @AuthenticationPrincipal AccountPrincipal principal) {
        userService.follow(principal, id);
        return ApiResponse.success("User followed.");
    }

    @DeleteMapping("/{id}/follow")
    @Operation(summary = "Unfollow a user")
    public ApiResponse<String> unfollow(
            @PathVariable Long id, @AuthenticationPrincipal AccountPrincipal principal) {
        userService.unfollow(principal, id);
        return ApiResponse.success("User unfollowed.");
    }

    @GetMapping("/{id}/followers")
    @Operation(summary = "List a user's followers")
    public ApiResponse<Page<UserSummaryDto>> followers(
            @PathVariable Long id,
            @AuthenticationPrincipal AccountPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(userService.followers(id, principal, pageable));
    }

    @GetMapping("/{id}/following")
    @Operation(summary = "List accounts a user follows")
    public ApiResponse<Page<UserSummaryDto>> following(
            @PathVariable Long id,
            @AuthenticationPrincipal AccountPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(userService.following(id, principal, pageable));
    }
}
