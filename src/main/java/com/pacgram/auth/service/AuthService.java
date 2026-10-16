package com.pacgram.auth.service;

import com.pacgram.auth.dto.AuthResponse;
import com.pacgram.auth.dto.ChangePasswordRequest;
import com.pacgram.auth.dto.CurrentUserDto;
import com.pacgram.auth.dto.ForgotPasswordRequest;
import com.pacgram.auth.dto.LoginRequest;
import com.pacgram.auth.dto.RegisterRequest;
import com.pacgram.auth.dto.ResetPasswordRequest;
import com.pacgram.auth.entity.PasswordResetToken;
import com.pacgram.auth.entity.UserAccount;
import com.pacgram.auth.repository.PasswordResetTokenRepository;
import com.pacgram.auth.repository.UserAccountRepository;
import com.pacgram.common.exception.ApiException;
import com.pacgram.common.exception.DuplicateException;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.common.security.JwtService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS = "Invalid username/email or password";
    private final UserAccountRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String mailFrom;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserAccountRepository userRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JavaMailSender mailSender,
            @Value("${pacgram.frontend-url:http://localhost:3000}") String frontendUrl,
            @Value("${spring.mail.username:}") String mailFrom) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.mailFrom = mailFrom;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().toLowerCase(java.util.Locale.ROOT);
        String email = request.email().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateException("DUPLICATE_USERNAME", "Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateException("DUPLICATE_EMAIL", "Email is already registered");
        }
        UserAccount user = userRepository.save(new UserAccount(
                request.name().trim(), username, email, passwordEncoder.encode(request.password())));
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        UserAccount user = userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())
                || !"USER".equals(user.getRole())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", INVALID_CREDENTIALS);
        }
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED",
                    "This account is currently unavailable. Contact PacGram support.");
        }
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public CurrentUserDto currentUser(AccountPrincipal principal) {
        UserAccount user = findUser(principal.userId());
        return new CurrentUserDto(user.getId(), user.getUsername(), user.getName(), user.getEmail());
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email().trim())
                .ifPresent(user -> {
                    resetTokenRepository.deleteByUserId(user.getId());
                    byte[] tokenBytes = new byte[32];
                    secureRandom.nextBytes(tokenBytes);
                    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
                    resetTokenRepository.save(new PasswordResetToken(
                            user, hash(token), Instant.now().plus(30, ChronoUnit.MINUTES)));
                    sendResetEmail(user.getEmail(), token);
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken reset = resetTokenRepository.findByTokenHash(hash(request.token()))
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Password reset token is invalid or expired"));
        reset.getUser().updatePasswordHash(passwordEncoder.encode(request.newPassword()));
        resetTokenRepository.delete(reset);
    }

    @Transactional
    public void changePassword(AccountPrincipal principal, ChangePasswordRequest request) {
        UserAccount user = findUser(principal.userId());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "Current password is incorrect");
        }
        user.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
        resetTokenRepository.deleteByUserId(user.getId());
    }

    private AuthResponse authResponse(UserAccount user) {
        return new AuthResponse(
                jwtService.issueToken(user.getId(), user.getUsername()),
                user.getId(), user.getUsername(), user.getName(), user.getEmail());
    }

    private UserAccount findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User account was not found"));
    }

    private void sendResetEmail(String email, String token) {
        String link = frontendUrl.replaceAll("/+$", "") + "/forgot-password?token=" + token;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            if (!mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }
            helper.setTo(email);
            helper.setSubject("Reset your PacGram password");
            helper.setText("Use this link within 30 minutes to reset your password: " + link, false);
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Unable to send password reset email", exception);
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
