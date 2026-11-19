UPDATE products
SET image_url = CASE name
    WHEN 'Hamburguesa de la casa' THEN 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=960&q=85'
    WHEN 'Pollo a la plancha' THEN 'https://images.unsplash.com/photo-1532550907401-a500c9a57435?auto=format&fit=crop&w=960&q=85'
    WHEN 'Pasta al pesto' THEN 'https://images.unsplash.com/photo-1473093295043-cdd812d0e601?auto=format&fit=crop&w=960&q=85'
    WHEN 'Limonada natural' THEN 'https://images.unsplash.com/photo-1546173159-315724a31696?auto=format&fit=crop&w=960&q=85'
    WHEN 'Jugo de mango' THEN 'https://images.unsplash.com/photo-1546173159-315724a31696?auto=format&fit=crop&w=960&q=85'
    WHEN 'Cafe americano' THEN 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=960&q=85'
    ELSE image_url
END
WHERE name IN (
    'Hamburguesa de la casa',
    'Pollo a la plancha',
    'Pasta al pesto',
    'Limonada natural',
    'Jugo de mango',
    'Cafe americano'
)
AND (image_url IS NULL OR image_url = '');

INSERT INTO products (name, description, category, image_url, price, unit_cost, stock, active)
SELECT seed.name, seed.description, seed.category, seed.image_url, seed.price, 0.00, seed.stock, TRUE
FROM (
    SELECT 'Ajiaco santafereño' AS name, 'Sopa tradicional de papa y pollo, con mazorca, crema, alcaparras y aguacate.' AS description, 'PLATO' AS category, 'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=960&q=85' AS image_url, 28000.00 AS price, 12 AS stock
    UNION ALL SELECT 'Bandeja paisa', 'Frijoles, arroz, carne molida, chicharrón, huevo, plátano maduro y arepa.', 'PLATO', 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=960&q=85', 39000.00, 14
    UNION ALL SELECT 'Salmón a la plancha', 'Filete de salmón a la plancha con puré de papa y vegetales.', 'PLATO', 'https://images.unsplash.com/photo-1467003909585-2f8a72700288?auto=format&fit=crop&w=960&q=85', 42000.00, 12
    UNION ALL SELECT 'Trucha al ajillo', 'Trucha dorada con salsa de ajo, arroz y ensalada fresca.', 'PLATO', 'https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?auto=format&fit=crop&w=960&q=85', 36000.00, 12
    UNION ALL SELECT 'Lomo de res a la parrilla', 'Lomo de res a la parrilla con papas rústicas y ensalada.', 'PLATO', 'https://images.unsplash.com/photo-1600891964599-f61ba0e24092?auto=format&fit=crop&w=960&q=85', 39000.00, 12
    UNION ALL SELECT 'Pechuga en salsa de champiñones', 'Pechuga jugosa con salsa cremosa de champiñones, arroz y vegetales.', 'PLATO', 'https://images.unsplash.com/photo-1532550907401-a500c9a57435?auto=format&fit=crop&w=960&q=85', 32000.00, 15
    UNION ALL SELECT 'Arroz cremoso de camarones', 'Arroz cremoso con camarones, hierbas frescas y un toque de limón.', 'PLATO', 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=960&q=85', 38000.00, 12
    UNION ALL SELECT 'Cazuela de mariscos', 'Camarones, calamar y pescado en salsa cremosa de coco, acompañados de arroz.', 'PLATO', 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=960&q=85', 42000.00, 10
    UNION ALL SELECT 'Risotto de hongos', 'Arroz arborio cremoso con hongos salteados y queso parmesano.', 'PLATO', 'https://images.unsplash.com/photo-1476124369491-e7addf5db371?auto=format&fit=crop&w=960&q=85', 29000.00, 12
    UNION ALL SELECT 'Lasaña boloñesa', 'Capas de pasta con ragú de res, salsa bechamel y queso gratinado.', 'PLATO', 'https://images.unsplash.com/photo-1574894709920-11b28e7367e3?auto=format&fit=crop&w=960&q=85', 29000.00, 14
    UNION ALL SELECT 'Tacos de carne', 'Tres tortillas de maíz con res sazonada, pico de gallo, aguacate y salsa.', 'PLATO', 'https://images.unsplash.com/photo-1551504734-5ee1c4a1479b?auto=format&fit=crop&w=960&q=85', 26000.00, 16
    UNION ALL SELECT 'Ensalada César con pollo', 'Lechuga fresca, pollo a la plancha, parmesano, crutones y aderezo César.', 'PLATO', 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=960&q=85', 24000.00, 14
    UNION ALL SELECT 'Bowl vegetariano', 'Quinua, vegetales asados, aguacate y aderezo de limón.', 'PLATO', 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=960&q=85', 22000.00, 14
    UNION ALL SELECT 'Costillas BBQ', 'Costillas de cerdo en salsa BBQ, servidas con papa criolla y ensalada.', 'PLATO', 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=960&q=85', 36000.00, 12
    UNION ALL SELECT 'Empanadas de carne', 'Tres empanadas de maíz rellenas de carne, con ají de la casa.', 'OTRO', 'https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=960&q=85', 11000.00, 30
    UNION ALL SELECT 'Patacones con hogao', 'Patacones crocantes servidos con hogao de tomate y cebolla.', 'OTRO', 'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=960&q=85', 13000.00, 22
    UNION ALL SELECT 'Papas criollas con suero', 'Papas criollas doradas con suero costeño y cilantro.', 'OTRO', 'https://images.unsplash.com/photo-1518013431117-eb1465fa5752?auto=format&fit=crop&w=960&q=85', 9500.00, 22
    UNION ALL SELECT 'Torta de chocolate', 'Torta húmeda de chocolate con ganache y frutos rojos.', 'OTRO', 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=960&q=85', 12000.00, 12
    UNION ALL SELECT 'Cheesecake de maracuyá', 'Cheesecake cremoso con salsa fresca de maracuyá.', 'OTRO', 'https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&w=960&q=85', 14000.00, 12
    UNION ALL SELECT 'Brownie con helado', 'Brownie de chocolate tibio con una bola de helado de vainilla.', 'OTRO', 'https://images.unsplash.com/photo-1606313564200-e75d5e30476c?auto=format&fit=crop&w=960&q=85', 13000.00, 12
    UNION ALL SELECT 'Limonada de coco', 'Limonada natural con leche de coco, servida bien fría.', 'BEBIDA', 'https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=960&q=85', 9500.00, 24
    UNION ALL SELECT 'Limonada de panela', 'Limón fresco mezclado con panela colombiana y hielo.', 'BEBIDA', 'https://images.unsplash.com/photo-1546173159-315724a31696?auto=format&fit=crop&w=960&q=85', 6500.00, 25
    UNION ALL SELECT 'Jugo de mora', 'Moras frescas licuadas, servidas en agua o leche.', 'BEBIDA', 'https://images.unsplash.com/photo-1546173159-315724a31696?auto=format&fit=crop&w=960&q=85', 8000.00, 25
    UNION ALL SELECT 'Jugo de maracuyá', 'Pulpa de maracuyá natural, dulce y refrescante.', 'BEBIDA', 'https://images.unsplash.com/photo-1622597467836-f3285f2131b8?auto=format&fit=crop&w=960&q=85', 8000.00, 25
    UNION ALL SELECT 'Jugo de guanábana', 'Jugo cremoso de guanábana preparado al momento.', 'BEBIDA', 'https://images.unsplash.com/photo-1600271886742-f049cd451bba?auto=format&fit=crop&w=960&q=85', 8500.00, 20
    UNION ALL SELECT 'Jugo de lulo', 'Jugo natural de lulo con su sabor cítrico característico.', 'BEBIDA', 'https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=960&q=85', 8500.00, 20
    UNION ALL SELECT 'Jugo de naranja', 'Naranjas recién exprimidas, sin conservantes.', 'BEBIDA', 'https://images.unsplash.com/photo-1600271886742-f049cd451bba?auto=format&fit=crop&w=960&q=85', 7500.00, 25
    UNION ALL SELECT 'Agua sin gas', 'Agua purificada fría en botella de 600 ml.', 'BEBIDA', 'https://images.unsplash.com/photo-1523362628745-0c100150b504?auto=format&fit=crop&w=960&q=85', 4000.00, 40
    UNION ALL SELECT 'Agua con gas', 'Agua mineral con gas, fría en botella de 600 ml.', 'BEBIDA', 'https://images.unsplash.com/photo-1629203851122-3726ecdf080e?auto=format&fit=crop&w=960&q=85', 5000.00, 30
    UNION ALL SELECT 'Gaseosa personal', 'Gaseosa fría en presentación personal de 400 ml.', 'BEBIDA', 'https://images.unsplash.com/photo-1629203851122-3726ecdf080e?auto=format&fit=crop&w=960&q=85', 5000.00, 40
    UNION ALL SELECT 'Té frío de durazno', 'Té negro frío con durazno y un toque de limón.', 'BEBIDA', 'https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&w=960&q=85', 6500.00, 20
    UNION ALL SELECT 'Cappuccino', 'Espresso con leche vaporizada y espuma cremosa.', 'BEBIDA', 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=960&q=85', 7000.00, 25
    UNION ALL SELECT 'Chocolate caliente', 'Chocolate colombiano caliente preparado con leche.', 'BEBIDA', 'https://images.unsplash.com/photo-1517578239113-b03992dcdd25?auto=format&fit=crop&w=960&q=85', 6500.00, 20
    UNION ALL SELECT 'Soda de frutos rojos', 'Soda artesanal con frutos rojos, limón y hierbabuena.', 'BEBIDA', 'https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=960&q=85', 8500.00, 20
    UNION ALL SELECT 'Aromática de hierbabuena', 'Infusión caliente de hierbabuena fresca.', 'BEBIDA', 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=960&q=85', 5000.00, 20
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM products existing WHERE existing.name = seed.name);
