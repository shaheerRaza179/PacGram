CREATE TABLE conversations (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_one_id BIGINT NOT NULL,
    user_two_id BIGINT NOT NULL,
    last_message_at DATETIME2 NOT NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT FK_conversations_user_one FOREIGN KEY (user_one_id) REFERENCES users (id),
    CONSTRAINT FK_conversations_user_two FOREIGN KEY (user_two_id) REFERENCES users (id),
    CONSTRAINT UQ_conversations_participants UNIQUE (user_one_id, user_two_id),
    CONSTRAINT CK_conversations_ordered_users CHECK (user_one_id < user_two_id)
);

CREATE INDEX IX_conversations_user_one_activity ON conversations (user_one_id, last_message_at DESC);
CREATE INDEX IX_conversations_user_two_activity ON conversations (user_two_id, last_message_at DESC);

CREATE TABLE direct_messages (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message_text NVARCHAR(2000) NOT NULL,
    created_at DATETIME2 NOT NULL,
    read_at DATETIME2 NULL,
    CONSTRAINT FK_direct_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT FK_direct_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id)
);

CREATE INDEX IX_direct_messages_conversation_created ON direct_messages (conversation_id, created_at);
CREATE INDEX IX_direct_messages_unread ON direct_messages (conversation_id, read_at);
