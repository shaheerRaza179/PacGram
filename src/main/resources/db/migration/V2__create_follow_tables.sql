CREATE TABLE user_follows (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    followed_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT FK_user_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id),
    CONSTRAINT FK_user_follows_followed FOREIGN KEY (followed_id) REFERENCES users (id),
    CONSTRAINT UQ_user_follows_pair UNIQUE (follower_id, followed_id),
    CONSTRAINT CK_user_follows_not_self CHECK (follower_id <> followed_id)
);

CREATE INDEX IX_user_follows_followed_created ON user_follows (followed_id, created_at DESC);
CREATE INDEX IX_user_follows_follower_created ON user_follows (follower_id, created_at DESC);
