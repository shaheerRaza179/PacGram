package com.pacgram.messaging.entity;

import com.pacgram.auth.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "conversations", uniqueConstraints =
        @UniqueConstraint(name = "UQ_conversations_participants", columnNames = {"user_one_id", "user_two_id"}))
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_one_id", nullable = false)
    private UserAccount userOne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_two_id", nullable = false)
    private UserAccount userTwo;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Conversation() {
    }

    public Conversation(UserAccount userOne, UserAccount userTwo) {
        this.userOne = userOne;
        this.userTwo = userTwo;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        lastMessageAt = now;
    }

    @PreUpdate
    void onUpdate() {
        if (lastMessageAt == null) {
            lastMessageAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public UserAccount getUserOne() { return userOne; }
    public UserAccount getUserTwo() { return userTwo; }
    public Instant getLastMessageAt() { return lastMessageAt; }
    public void markActive(Instant at) { lastMessageAt = at; }

    public boolean hasParticipant(Long userId) {
        return userOne.getId().equals(userId) || userTwo.getId().equals(userId);
    }

    public UserAccount otherParticipant(Long userId) {
        return userOne.getId().equals(userId) ? userTwo : userOne;
    }
}
