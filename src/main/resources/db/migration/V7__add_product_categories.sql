ALTER TABLE products ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'PLATO' AFTER description;

UPDATE products
SET category = 'BEBIDA'
WHERE LOWER(name) LIKE '%limonada%'
   OR LOWER(name) LIKE '%jugo%'
   OR LOWER(name) LIKE '%cafe%'
   OR LOWER(name) LIKE '%café%';