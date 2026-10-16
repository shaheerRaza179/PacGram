package com.pacgram.admin.service;

import com.pacgram.admin.dto.AdminLoginDto;
import com.pacgram.admin.dto.AdminLoginRequest;
import com.pacgram.admin.repository.AdminAccountRepository;
import com.pacgram.auth.entity.UserAccount;
import com.pacgram.common.exception.ApiException;
import com.pacgram.common.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {
    private final AdminAccountRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminAuthService(
            AdminAccountRepository adminRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AdminLoginDto login(AdminLoginRequest request) {
        UserAccount account = adminRepository.findByUsername(request.username().trim())
                .filter(user -> "ACTIVE".equals(user.getAccountStatus()))
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw invalidCredentials();
        }
        return new AdminLoginDto(jwtService.issueAdministratorToken(account.getId(), account.getUsername()),
                account.getUsername(), "ADMIN");
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_ADMIN_CREDENTIALS",
                "Invalid admin username or password");
    }
}
