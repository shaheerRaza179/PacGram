package com.pacgram.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 80)
    private String name;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @Column(name = "cover_url", length = 2048)
    private String coverUrl;

    @Column(length = 500)
    private String bio;

    @Column(name = "private_account", nullable = false)
    private boolean privateAccount;

    @Column(name = "activity_status", nullable = false)
    private boolean activityStatus = true;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "account_status", nullable = false, length = 20)
    private String accountStatus = "ACTIVE";

    @Column(name = "account_role", nullable = false, length = 20)
    private String role = "USER";

    @Column(name = "notify_likes", nullable = false)
    private boolean notifyLikes = true;

    @Column(name = "notify_comments", nullable = false)
    private boolean notifyComments = true;

    @Column(name = "notify_follows", nullable = false)
    private boolean notifyFollows = true;

    @Column(name = "notify_messages", nullable = false)
    private boolean notifyMessages = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccount() {
    }

    public UserAccount(String name, String username, String email, String passwordHash) {
        this.name = name;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getCoverUrl() { return coverUrl; }
    public String getBio() { return bio; }
    public boolean isPrivateAccount() { return privateAccount; }
    public boolean isActivityStatus() { return activityStatus; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public String getAccountStatus() { return accountStatus; }
    public String getRole() { return role; }
    public boolean isNotifyLikes() { return notifyLikes; }
    public boolean isNotifyComments() { return notifyComments; }
    public boolean isNotifyFollows() { return notifyFollows; }
    public boolean isNotifyMessages() { return notifyMessages; }
    public Instant getCreatedAt() { return createdAt; }

    public void updatePasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void recordPresence(Instant at) { this.lastSeenAt = at; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    public void setRole(String role) { this.role = role; }
    public void updateProfile(String name, String bio, String avatarUrl, String coverUrl) {
        this.name = name;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.coverUrl = coverUrl;
    }
    public void updateSettings(boolean privateAccount, boolean activityStatus,
                               boolean notifyLikes, boolean notifyComments,
                               boolean notifyFollows, boolean notifyMessages) {
        this.privateAccount = privateAccount;
        this.activityStatus = activityStatus;
        this.notifyLikes = notifyLikes;
        this.notifyComments = notifyComments;
        this.notifyFollows = notifyFollows;
        this.notifyMessages = notifyMessages;
    }
}
