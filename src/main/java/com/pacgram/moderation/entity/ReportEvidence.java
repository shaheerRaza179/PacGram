package com.pacgram.moderation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "report_evidence")
public class ReportEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private UserReport report;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReportEvidence() {
    }

    public ReportEvidence(UserReport report, String fileName, String originalName,
                         String contentType, long fileSize) {
        this.report = report;
        this.fileName = fileName;
        this.originalName = originalName;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserReport getReport() { return report; }
    public String getFileName() { return fileName; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public Instant getCreatedAt() { return createdAt; }
}
