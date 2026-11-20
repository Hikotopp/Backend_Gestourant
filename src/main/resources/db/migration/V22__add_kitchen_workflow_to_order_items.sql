ALTER TABLE order_items
    ADD COLUMN kitchen_status VARCHAR(20) NOT NULL DEFAULT 'ENTREGADO',
    ADD COLUMN kitchen_batch_id VARCHAR(36) NULL,
    ADD COLUMN kitchen_submitted_at DATETIME NULL,
    ADD COLUMN order_origin VARCHAR(10) NOT NULL DEFAULT 'STAFF';
