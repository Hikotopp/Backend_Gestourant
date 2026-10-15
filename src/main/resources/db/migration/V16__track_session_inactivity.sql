ALTER TABLE users
    ADD COLUMN last_activity_at DATETIME NULL,
    ADD COLUMN active_session_locked TINYINT(1) NOT NULL DEFAULT 0;

CREATE INDEX idx_users_last_activity_session ON users (last_activity_at, active_session_id);

UPDATE users
SET last_activity_at = CURRENT_TIMESTAMP
WHERE active_session_id IS NOT NULL;
