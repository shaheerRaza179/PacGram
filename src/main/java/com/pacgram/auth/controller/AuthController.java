package com.pacgram.auth.controller;

import com.pacgram.auth.dto.AuthResponse;
import com.pacgram.auth.dto.ChangePasswordRequest;
import com.pacgram.auth.dto.CurrentUserDto;
import com.pacgram.auth.dto.ForgotPasswordRequest;
import com.pacgram.auth.dto.LoginRequest;
import com.pacgram.auth.dto.RegisterRequest;
import com.pacgram.auth.dto.ResetPasswordRequest;
import com.pacgram.auth.service.AuthService;
import com.pacgram.common.api.ApiResponse;
import com.pacgram.common.security.AccountPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register an account")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email or username")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's identity")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<CurrentUserDto> currentUser(@AuthenticationPrincipal AccountPrincipal principal) {
        return ApiResponse.success(authService.currentUser(principal));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Email a short-lived password reset link")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ApiResponse.success("If the account exists, password reset instructions have been sent.");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset a password with an emailed token")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success("Password has been reset.");
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change the authenticated user's password")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<String> changePassword(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal, request);
        return ApiResponse.success("Password has been changed.");
    }
}
