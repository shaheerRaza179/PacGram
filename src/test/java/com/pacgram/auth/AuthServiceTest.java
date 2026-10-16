package com.pacgram.auth;

import com.pacgram.auth.dto.LoginRequest;
import com.pacgram.auth.dto.RegisterRequest;
import com.pacgram.auth.entity.UserAccount;
import com.pacgram.auth.repository.PasswordResetTokenRepository;
import com.pacgram.auth.repository.UserAccountRepository;
import com.pacgram.auth.service.AuthService;
import com.pacgram.common.exception.DuplicateException;
import com.pacgram.common.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private UserAccountRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserAccountRepository.class);
        PasswordResetTokenRepository resetTokens = mock(PasswordResetTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        authService = new AuthService(userRepository, resetTokens, passwordEncoder,
                jwtService, mailSender, "http://localhost:3000", "");
    }

    @Test
    void registerNormalizesIdentityAndReturnsSignedToken() {
        UserAccount savedUser = mock(UserAccount.class);
        when(userRepository.save(any(UserAccount.class))).thenReturn(savedUser);
        when(savedUser.getId()).thenReturn(42L);
        when(savedUser.getUsername()).thenReturn("pacgram_user");
        when(savedUser.getName()).thenReturn("PacGram User");
        when(savedUser.getEmail()).thenReturn("person@example.com");
        when(jwtService.issueToken(42L, "pacgram_user")).thenReturn("signed-token");

        var response = authService.register(new RegisterRequest(
                " PacGram User ", "PacGram_User", "PERSON@EXAMPLE.COM", "StrongPass123"));

        assertThat(response.token()).isEqualTo("signed-token");
        assertThat(response.username()).isEqualTo("pacgram_user");
        verify(passwordEncoder).encode("StrongPass123");
        verify(userRepository).existsByEmailIgnoreCase("person@example.com");
        verify(userRepository).existsByUsernameIgnoreCase("pacgram_user");
    }

    @Test
    void duplicateEmailIsReportedAsConflict() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest(
                "PacGram User", "pacgram_user", "person@example.com", "StrongPass123")))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    void loginLooksUpAndAuthenticatesUsernameOrEmailIdentifier() {
        UserAccount user = mock(UserAccount.class);
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("pacgram_user", "pacgram_user"))
                .thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(42L);
        when(user.getUsername()).thenReturn("pacgram_user");
        when(user.getName()).thenReturn("PacGram User");
        when(user.getEmail()).thenReturn("person@example.com");
        when(user.getPasswordHash()).thenReturn("bcrypt-hash");
        when(user.getRole()).thenReturn("USER");
        when(passwordEncoder.matches("StrongPass123", "bcrypt-hash")).thenReturn(true);
        when(jwtService.issueToken(42L, "pacgram_user")).thenReturn("signed-token");

        var response = authService.login(new LoginRequest("pacgram_user", "StrongPass123"));

        assertThat(response.token()).isEqualTo("signed-token");
        assertThat(response.userId()).isEqualTo(42L);
    }
}
