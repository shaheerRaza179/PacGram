package com.pacgram.admin.repository;

import com.pacgram.auth.entity.UserAccount;
import com.pacgram.auth.repository.UserAccountRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AdminAccountRepository {
    private final UserAccountRepository userRepository;

    public AdminAccountRepository(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<UserAccount> findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCaseAndRole(username, "ADMIN");
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

    public UserAccount create(String username, String encodedPassword) {
        UserAccount account = new UserAccount(
                "PacGram Administrator", username, username + "@pacgram.local", encodedPassword);
        account.setRole("ADMIN");
        return userRepository.save(account);
    }

    public UserAccount updatePassword(String username, String encodedPassword) {
        UserAccount account = findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Administrator account was not found"));
        account.updatePasswordHash(encodedPassword);
        return userRepository.save(account);
    }
}
