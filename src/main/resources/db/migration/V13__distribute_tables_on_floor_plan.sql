UPDATE restaurant_tables
SET floor_x = 14.00 + MOD(table_number - 1, 4) * 24.00,
    floor_y = 24.00 + FLOOR(MOD(table_number - 1, 16) / 4) * 21.00;
