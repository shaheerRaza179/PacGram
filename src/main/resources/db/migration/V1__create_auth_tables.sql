CREATE TABLE users (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    display_name NVARCHAR(80) NOT NULL,
    username NVARCHAR(30) NOT NULL,
    email NVARCHAR(254) NOT NULL,
    password_hash NVARCHAR(100) NOT NULL,
    avatar_url NVARCHAR(2048) NULL,
    cover_url NVARCHAR(2048) NULL,
    bio NVARCHAR(500) NULL,
    private_account BIT NOT NULL CONSTRAINT DF_users_private_account DEFAULT 0,
    activity_status BIT NOT NULL CONSTRAINT DF_users_activity_status DEFAULT 1,
    notify_likes BIT NOT NULL CONSTRAINT DF_users_notify_likes DEFAULT 1,
    notify_comments BIT NOT NULL CONSTRAINT DF_users_notify_comments DEFAULT 1,
    notify_follows BIT NOT NULL CONSTRAINT DF_users_notify_follows DEFAULT 1,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL
);

CREATE UNIQUE INDEX UX_users_username ON users (username);
CREATE UNIQUE INDEX UX_users_email ON users (email);

CREATE TABLE password_reset_tokens (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash NVARCHAR(64) NOT NULL,
    expires_at DATETIME2 NOT NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT FK_password_reset_tokens_users FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT UQ_password_reset_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX IX_password_reset_tokens_user_id ON password_reset_tokens (user_id);
