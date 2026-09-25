ALTER TABLE order_items
    ADD COLUMN removed_ingredients VARCHAR(500) NOT NULL DEFAULT '';
