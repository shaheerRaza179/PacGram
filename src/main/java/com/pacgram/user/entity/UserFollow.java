package com.pacgram.user.entity;

import com.pacgram.auth.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "user_follows", uniqueConstraints =
        @UniqueConstraint(name = "UQ_user_follows_pair", columnNames = {"follower_id", "followed_id"}))
public class UserFollow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private UserAccount follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followed_id", nullable = false)
    private UserAccount followed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserFollow() {
    }

    public UserFollow(UserAccount follower, UserAccount followed) {
        this.follower = follower;
        this.followed = followed;
        this.createdAt = Instant.now();
    }

    public UserAccount getFollower() { return follower; }
    public UserAccount getFollowed() { return followed; }
}
