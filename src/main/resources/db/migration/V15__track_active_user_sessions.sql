ALTER TABLE users
    ADD COLUMN active_session_id VARCHAR(36) NULL,
    ADD COLUMN active_session_expires_at DATETIME NULL;
