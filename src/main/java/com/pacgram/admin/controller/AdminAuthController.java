package com.pacgram.admin.controller;

import com.pacgram.admin.dto.AdminLoginDto;
import com.pacgram.admin.dto.AdminLoginRequest;
import com.pacgram.admin.service.AdminAuthService;
import com.pacgram.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AdminAuthService authService;

    public AdminAuthController(AdminAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AdminLoginDto> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
