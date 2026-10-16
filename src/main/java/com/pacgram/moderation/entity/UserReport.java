package com.pacgram.moderation.entity;

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

import java.time.Instant;

@Entity
@Table(name = "user_reports")
public class UserReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserAccount reporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private UserAccount reportedUser;

    @Column(nullable = false, length = 60)
    private String reason;

    @Column(length = 1000)
    private String details;

    @Column(name = "content_type", length = 30)
    private String contentType;

    @Column(name = "content_id")
    private Long contentId;

    @Column(name = "content_snapshot", length = 3000)
    private String contentSnapshot;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserReport() {
    }

    public UserReport(UserAccount reporter, UserAccount reportedUser, String reason, String details,
                      String contentType, Long contentId, String contentSnapshot) {
        this.reporter = reporter;
        this.reportedUser = reportedUser;
        this.reason = reason;
        this.details = details;
        this.contentType = contentType;
        this.contentId = contentId;
        this.contentSnapshot = contentSnapshot;
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
    public UserAccount getReporter() { return reporter; }
    public UserAccount getReportedUser() { return reportedUser; }
    public String getReason() { return reason; }
    public String getDetails() { return details; }
    public String getContentType() { return contentType; }
    public Long getContentId() { return contentId; }
    public String getContentSnapshot() { return contentSnapshot; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
