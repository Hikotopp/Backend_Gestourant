ALTER TABLE restaurant_tables
    ADD COLUMN floor_x DECIMAL(5,2) NOT NULL DEFAULT 15.00,
    ADD COLUMN floor_y DECIMAL(5,2) NOT NULL DEFAULT 15.00,
    ADD COLUMN qr_token VARCHAR(36) NULL,
    ADD COLUMN bill_requested BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE restaurant_tables
SET floor_x = 12.00 + MOD(table_number - 1, 4) * 24.00,
    floor_y = 18.00 + FLOOR(MOD(table_number - 1, 16) / 4) * 24.00;
UPDATE restaurant_tables SET qr_token = UUID() WHERE qr_token IS NULL;
ALTER TABLE restaurant_tables
    MODIFY COLUMN qr_token VARCHAR(36) NOT NULL,
    ADD CONSTRAINT uk_restaurant_tables_qr_token UNIQUE (qr_token);

CREATE TABLE guest_order_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    table_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_guest_requests_table FOREIGN KEY (table_id) REFERENCES restaurant_tables(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE guest_order_request_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    removed_ingredients VARCHAR(500) NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    CONSTRAINT fk_guest_request_items_request FOREIGN KEY (request_id) REFERENCES guest_order_requests(id),
    CONSTRAINT fk_guest_request_items_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
