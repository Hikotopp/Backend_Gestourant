ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'EMPLEADO' AFTER password_hash;

CREATE TABLE restaurant_tables (
    id BIGINT NOT NULL AUTO_INCREMENT,
    table_number INT NOT NULL,
    seats INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'LIBRE',
    PRIMARY KEY (id),
    CONSTRAINT uk_restaurant_tables_number UNIQUE (table_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    table_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',
    total DECIMAL(12,2) NOT NULL DEFAULT 0,
    opened_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_table FOREIGN KEY (table_id) REFERENCES restaurant_tables(id),
    CONSTRAINT fk_orders_user FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    invoice_number VARCHAR(40) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoices_number UNIQUE (invoice_number),
    CONSTRAINT uk_invoices_order UNIQUE (order_id),
    CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
