ALTER TABLE products
    ADD COLUMN unit_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00;

ALTER TABLE order_items
    ADD COLUMN unit_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN product_name_snapshot VARCHAR(120) NOT NULL DEFAULT '';

UPDATE order_items oi
JOIN products p ON p.id = oi.product_id
SET oi.product_name_snapshot = p.name,
    oi.unit_cost = p.unit_cost;

CREATE TABLE product_returns (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    processed_by BIGINT NOT NULL,
    reason VARCHAR(40) NOT NULL,
    notes VARCHAR(500) NOT NULL DEFAULT '',
    refund_method VARCHAR(20) NOT NULL,
    refund_total DECIMAL(12,2) NOT NULL,
    returned_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_returns_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_product_returns_user FOREIGN KEY (processed_by) REFERENCES users(id),
    INDEX ix_product_returns_returned_at (returned_at),
    INDEX ix_product_returns_invoice (invoice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_return_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    return_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name_snapshot VARCHAR(120) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    refund_amount DECIMAL(12,2) NOT NULL,
    unit_cost DECIMAL(12,2) NOT NULL,
    restocked BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_return_items_return FOREIGN KEY (return_id) REFERENCES product_returns(id),
    CONSTRAINT fk_product_return_items_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    CONSTRAINT fk_product_return_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_product_return_items_quantity CHECK (quantity > 0),
    INDEX ix_product_return_items_order_item (order_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_losses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    reported_by BIGINT NOT NULL,
    product_name_snapshot VARCHAR(120) NOT NULL,
    reason VARCHAR(40) NOT NULL,
    notes VARCHAR(500) NOT NULL DEFAULT '',
    quantity INT NOT NULL,
    unit_cost DECIMAL(12,2) NOT NULL,
    total_cost DECIMAL(12,2) NOT NULL,
    reported_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_inventory_losses_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_inventory_losses_user FOREIGN KEY (reported_by) REFERENCES users(id),
    CONSTRAINT chk_inventory_losses_quantity CHECK (quantity > 0),
    INDEX ix_inventory_losses_reported_at (reported_at),
    INDEX ix_inventory_losses_reported_by (reported_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
