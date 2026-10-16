ALTER TABLE users
    ADD last_seen_at DATETIME2 NULL,
        account_status NVARCHAR(20) NOT NULL
            CONSTRAINT DF_users_account_status DEFAULT 'ACTIVE';

CREATE INDEX IX_users_account_status_last_seen
    ON users (account_status, activity_status, last_seen_at DESC);

CREATE TABLE user_reports (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    reported_user_id BIGINT NOT NULL,
    reason NVARCHAR(60) NOT NULL,
    details NVARCHAR(1000) NULL,
    content_type NVARCHAR(30) NULL,
    content_id BIGINT NULL,
    content_snapshot NVARCHAR(3000) NULL,
    status NVARCHAR(20) NOT NULL
        CONSTRAINT DF_user_reports_status DEFAULT 'PENDING',
    resolution_note NVARCHAR(1000) NULL,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    CONSTRAINT FK_user_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT FK_user_reports_reported_user FOREIGN KEY (reported_user_id) REFERENCES users (id),
    CONSTRAINT CK_user_reports_not_self CHECK (reporter_id <> reported_user_id),
    CONSTRAINT CK_user_reports_status CHECK (status IN ('PENDING', 'IN_REVIEW', 'RESOLVED', 'DISMISSED'))
);

CREATE INDEX IX_user_reports_status_created
    ON user_reports (status, created_at DESC);
CREATE INDEX IX_user_reports_reported_user
    ON user_reports (reported_user_id, created_at DESC);
