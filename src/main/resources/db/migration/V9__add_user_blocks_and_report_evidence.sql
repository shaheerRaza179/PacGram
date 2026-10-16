CREATE TABLE user_blocks (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT FK_user_blocks_blocker FOREIGN KEY (blocker_id) REFERENCES users (id),
    CONSTRAINT FK_user_blocks_blocked FOREIGN KEY (blocked_id) REFERENCES users (id),
    CONSTRAINT UQ_user_blocks_pair UNIQUE (blocker_id, blocked_id),
    CONSTRAINT CK_user_blocks_not_self CHECK (blocker_id <> blocked_id)
);

CREATE INDEX IX_user_blocks_blocked ON user_blocks (blocked_id, blocker_id);

CREATE TABLE report_evidence (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    report_id BIGINT NOT NULL,
    file_name NVARCHAR(255) NOT NULL,
    original_name NVARCHAR(255) NOT NULL,
    content_type NVARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT FK_report_evidence_report FOREIGN KEY (report_id)
        REFERENCES user_reports (id) ON DELETE CASCADE
);

CREATE INDEX IX_report_evidence_report ON report_evidence (report_id, created_at);
