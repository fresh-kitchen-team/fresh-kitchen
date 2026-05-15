INSERT INTO ingredient_catalog (name, category, default_storage_type, emoji)
VALUES
    ('토마토', 'VEGETABLE', 'FRIDGE', '🍅'),
    ('계란', 'DAIRY', 'FRIDGE', '🥚'),
    ('우유', 'DAIRY', 'FRIDGE', '🥛'),
    ('양파', 'VEGETABLE', 'PANTRY', '🧅'),
    ('사과', 'FRUIT', 'FRIDGE', '🍎'),
    ('돼지고기', 'MEAT', 'FRIDGE', '🥩'),
    ('닭고기', 'MEAT', 'FRIDGE', '🍗'),
    ('새우', 'SEAFOOD', 'FREEZER', '🍤'),
    ('두부', 'ETC', 'FRIDGE', '◻️'),
    ('김치', 'ETC', 'FRIDGE', '🥬')
ON CONFLICT (name) DO UPDATE SET
    category = EXCLUDED.category,
    default_storage_type = EXCLUDED.default_storage_type,
    emoji = EXCLUDED.emoji,
    updated_at = CURRENT_TIMESTAMP;

SELECT setval(
    pg_get_serial_sequence('ingredient_catalog', 'id'),
    GREATEST((SELECT MAX(id) FROM ingredient_catalog), 1)
);
