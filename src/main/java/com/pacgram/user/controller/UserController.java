package com.pacgram.user.controller;

import com.pacgram.common.api.ApiResponse;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.common.storage.ImageStorageService;
import com.pacgram.user.dto.UpdateProfileRequest;
import com.pacgram.user.dto.AccountSettingsDto;
import com.pacgram.user.dto.AccountSettingsRequest;
import com.pacgram.user.dto.DeleteAccountRequest;
import com.pacgram.user.service.AccountService;
import com.pacgram.user.dto.UserProfileDto;
import com.pacgram.user.dto.UserSummaryDto;
import com.pacgram.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users and follows")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class UserController {
    private final UserProfileService userService;
    private final AccountService accountService;
    private final ImageStorageService imageStorageService;

    public UserController(
            UserProfileService userService,
            AccountService accountService,
            ImageStorageService imageStorageService) {
        this.userService = userService;
        this.accountService = accountService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ApiResponse<UserProfileDto> me(@AuthenticationPrincipal AccountPrincipal principal) {
        return ApiResponse.success(userService.getOwnProfile(principal));
    }

    @PutMapping("/me/presence")
    @Operation(summary = "Update the authenticated user's last-active time")
    public ApiResponse<String> presence(@AuthenticationPrincipal AccountPrincipal principal) {
        userService.recordPresence(principal);
        return ApiResponse.success("Presence updated.");
    }

    @GetMapping("/me/blocked")
    @Operation(summary = "List accounts blocked by the authenticated user")
    public ApiResponse<Page<UserSummaryDto>> blockedUsers(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success(userService.blockedUsers(principal, pageable));
    }

    @PostMapping("/{id}/block")
    @Operation(summary = "Block an account and remove mutual follows")
    public ApiResponse<String> block(
            @PathVariable Long id, @AuthenticationPrincipal AccountPrincipal principal) {
        userService.block(principal, id);
        return ApiResponse.success("User blocked.");
    }

    @DeleteMapping("/{id}/block")
    @Operation(summary = "Unblock an account")
    public ApiResponse<String> unblock(
            @PathVariable Long id, @AuthenticationPrincipal AccountPrincipal principal) {
        userService.unblock(principal, id);
        return ApiResponse.success("User unblocked.");
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user's profile")
    public ApiResponse<UserProfileDto> updateProfile(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(principal, request));
    }

    @GetMapping("/me/settings")
    @Operation(summary = "Get account privacy and notification settings")
    public ApiResponse<AccountSettingsDto> settings(@AuthenticationPrincipal AccountPrincipal principal) {
        return ApiResponse.success(userService.settings(principal));
    }

    @PutMapping("/me/settings")
    @Operation(summary = "Update account privacy and notification settings")
    public ApiResponse<AccountSettingsDto> updateSettings(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody AccountSettingsRequest request) {
        return ApiResponse.success(userService.updateSettings(principal, request));
    }

    @PostMapping("/me/{imageType}")
    @Operation(summary = "Upload the authenticated user's avatar or cover image")
    public ApiResponse<UserProfileDto> uploadImage(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String imageType,
            @RequestParam("file") MultipartFile file) {
        if (!"avatar".equals(imageType) && !"cover".equals(imageType)) {
            throw new com.pacgram.common.exception.ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_IMAGE_TYPE", "Image type must be avatar or cover");
        }
        String imageUrl = imageStorageService.storeImage(file);
        return ApiResponse.success(userService.updateImage(principal, imageType, imageUrl));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username or display name")
    public ApiResponse<Page<UserSummaryDto>> search(
            @RequestParam @NotBlank @Size(max = 80) String q,
            @AuthenticationPrincipal AccountPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(userService.search(q, principal, pageable));
    }

    @GetMapping("/suggested")
    @Operation(summary = "Get accounts the authenticated user does not follow")
    public ApiResponse<Page<UserSummaryDto>> suggested(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(userService.suggestions(principal, pageable));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get a public user profile")
    public ApiResponse<UserProfileDto> profile(
            @PathVariable String username,
            @AuthenticationPrincipal AccountPrincipal principal) {
        return ApiResponse.success(userService.getProfile(username, principal));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete the authenticated user's account")
    public ApiResponse<String> deleteAccount(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody DeleteAccountRequest request) {
        accountService.deleteAccount(principal, request);
        return ApiResponse.success("Account deleted.");
    }
}
