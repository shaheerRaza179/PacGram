CREATE TABLE notifications (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    post_id BIGINT NULL,
    type NVARCHAR(20) NOT NULL,
    preview NVARCHAR(500) NULL,
    is_read BIT NOT NULL CONSTRAINT DF_notifications_read DEFAULT 0,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT FK_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT FK_notifications_actor FOREIGN KEY (actor_id) REFERENCES users (id),
    CONSTRAINT FK_notifications_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT CK_notifications_type CHECK (type IN (N'LIKE', N'COMMENT', N'FOLLOW', N'MESSAGE'))
);

CREATE INDEX IX_notifications_recipient_created ON notifications (recipient_id, created_at DESC);
CREATE INDEX IX_notifications_recipient_unread ON notifications (recipient_id, is_read);
