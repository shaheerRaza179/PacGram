ALTER TABLE users
    ADD account_role NVARCHAR(20) NOT NULL
        CONSTRAINT DF_users_account_role DEFAULT 'USER';

CREATE TABLE admin_audit_log (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    admin_username NVARCHAR(40) NOT NULL,
    action NVARCHAR(60) NOT NULL,
    target_type NVARCHAR(40) NOT NULL,
    target_id BIGINT NULL,
    description NVARCHAR(1000) NOT NULL,
    created_at DATETIME2 NOT NULL
);

CREATE INDEX IX_admin_audit_created ON admin_audit_log (created_at DESC);
