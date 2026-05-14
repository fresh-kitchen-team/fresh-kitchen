INSERT INTO ingredient_catalog (id, name, category, default_storage_type, emoji)
VALUES
    (1, '토마토', 'VEGETABLE', 'FRIDGE', '🍅'),
    (2, '계란', 'DAIRY', 'FRIDGE', '🥚'),
    (3, '우유', 'DAIRY', 'FRIDGE', '🥛'),
    (4, '양파', 'VEGETABLE', 'PANTRY', '🧅'),
    (5, '사과', 'FRUIT', 'FRIDGE', '🍎'),
    (6, '돼지고기', 'MEAT', 'FRIDGE', '🥩'),
    (7, '닭고기', 'MEAT', 'FRIDGE', '🍗'),
    (8, '새우', 'SEAFOOD', 'FREEZER', '🍤'),
    (9, '두부', 'ETC', 'FRIDGE', '◻️'),
    (10, '김치', 'ETC', 'FRIDGE', '🥬')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    category = EXCLUDED.category,
    default_storage_type = EXCLUDED.default_storage_type,
    emoji = EXCLUDED.emoji,
    updated_at = CURRENT_TIMESTAMP;

SELECT setval(
    pg_get_serial_sequence('ingredient_catalog', 'id'),
    GREATEST((SELECT MAX(id) FROM ingredient_catalog), 1)
);
