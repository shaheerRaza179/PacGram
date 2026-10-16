package com.pacgram.auth.repository;

import com.pacgram.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);
    Optional<UserAccount> findByUsernameIgnoreCase(String username);
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByIdAndAccountStatus(Long id, String accountStatus);
    boolean existsByIdAndRoleAndAccountStatus(Long id, String role, String accountStatus);
    java.util.Optional<UserAccount> findByUsernameIgnoreCaseAndRole(String username, String role);
}
