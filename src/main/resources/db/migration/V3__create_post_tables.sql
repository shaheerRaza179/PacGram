CREATE TABLE posts (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    caption NVARCHAR(2200) NULL,
    image_url NVARCHAR(2048) NULL,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    CONSTRAINT FK_posts_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT CK_posts_content CHECK (image_url IS NOT NULL OR caption IS NOT NULL)
);

CREATE INDEX IX_posts_author_created ON posts (author_id, created_at DESC);
CREATE INDEX IX_posts_created ON posts (created_at DESC);

CREATE TABLE post_likes (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT FK_post_likes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT FK_post_likes_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT UQ_post_likes_user_post UNIQUE (user_id, post_id)
);

CREATE INDEX IX_post_likes_post ON post_likes (post_id);

CREATE TABLE saved_posts (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT FK_saved_posts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT FK_saved_posts_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT UQ_saved_posts_user_post UNIQUE (user_id, post_id)
);

CREATE INDEX IX_saved_posts_user_created ON saved_posts (user_id, created_at DESC);

CREATE TABLE post_comments (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    body NVARCHAR(1000) NOT NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT FK_post_comments_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT FK_post_comments_post FOREIGN KEY (post_id) REFERENCES posts (id)
);

CREATE INDEX IX_post_comments_post_created ON post_comments (post_id, created_at);
