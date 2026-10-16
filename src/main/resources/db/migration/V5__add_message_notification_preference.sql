ALTER TABLE users
    ADD notify_messages BIT NOT NULL
        CONSTRAINT DF_users_notify_messages DEFAULT 1;
