ALTER TABLE guest_order_requests
    ADD COLUMN guest_session_hash VARCHAR(64) NULL AFTER resolved_at;

CREATE INDEX idx_guest_requests_table_session_created
    ON guest_order_requests (table_id, guest_session_hash, created_at);
