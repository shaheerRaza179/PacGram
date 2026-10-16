package com.pacgram.notification.entity;

import com.pacgram.auth.entity.UserAccount;
import com.pacgram.common.event.NotificationType;
import com.pacgram.post.entity.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserAccount recipient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private UserAccount actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(length = 500)
    private String preview;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(
            UserAccount recipient, UserAccount actor, Post post, NotificationType type, String preview) {
        this.recipient = recipient;
        this.actor = actor;
        this.post = post;
        this.type = type;
        this.preview = preview;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserAccount getRecipient() { return recipient; }
    public UserAccount getActor() { return actor; }
    public Post getPost() { return post; }
    public NotificationType getType() { return type; }
    public String getPreview() { return preview; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
    public void markRead() { read = true; }
}
