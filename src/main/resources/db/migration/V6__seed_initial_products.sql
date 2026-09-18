INSERT INTO products (name, description, price, stock, active)
SELECT seed.name, seed.description, seed.price, seed.stock, TRUE
FROM (
    SELECT 'Hamburguesa de la casa' AS name, 'Carne artesanal, queso, vegetales y papas.' AS description, 28000.00 AS price, 20 AS stock
    UNION ALL SELECT 'Pollo a la plancha', 'Pechuga a la plancha con arroz y ensalada.', 26000.00, 20
    UNION ALL SELECT 'Pasta al pesto', 'Pasta con salsa pesto, tomate cherry y parmesano.', 24000.00, 15
    UNION ALL SELECT 'Limonada natural', 'Limonada preparada al momento.', 7000.00, 30
    UNION ALL SELECT 'Jugo de mango', 'Jugo natural de mango.', 8000.00, 30
    UNION ALL SELECT 'Cafe americano', 'Cafe caliente de la casa.', 5000.00, 30
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM products);