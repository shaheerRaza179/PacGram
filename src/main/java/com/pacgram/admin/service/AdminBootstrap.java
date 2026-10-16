package com.pacgram.admin.service;

import com.pacgram.admin.repository.AdminAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminAccountRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String initialPassword;

    public AdminBootstrap(
            AdminAccountRepository adminRepository,
            PasswordEncoder passwordEncoder,
            @Value("${pacgram.admin.username:pacadmin}") String username,
            @Value("${pacgram.admin.password:}") String initialPassword) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.initialPassword = initialPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        var administrator = adminRepository.findByUsername(username);
        if (administrator.isPresent()) {
            if (!initialPassword.isBlank()
                    && !passwordEncoder.matches(initialPassword, administrator.get().getPasswordHash())) {
                adminRepository.updatePassword(username, passwordEncoder.encode(initialPassword));
            }
            return;
        }
        if (initialPassword.isBlank()) {
            log.warn("Admin account '{}' has not been provisioned. Set PACGRAM_ADMIN_PASSWORD to enable admin sign-in.",
                    username);
            return;
        }
        if (adminRepository.existsByUsername(username)) {
            throw new IllegalStateException(
                    "PACGRAM_ADMIN_USERNAME is already used by a non-administrator account");
        }
        adminRepository.create(username, passwordEncoder.encode(initialPassword));
    }
}
