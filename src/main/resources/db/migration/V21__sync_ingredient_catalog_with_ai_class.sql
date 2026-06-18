DELETE FROM ingredient_catalog
WHERE name IN (
    '다짐육',
    '레몬',
    '면',
    '물',
    '밥',
    '배추',
    '복숭아',
    '빵',
    '생크림',
    '소시지',
    '수박',
    '숙주',
    '연어',
    '오리고기',
    '요거트',
    '조개',
    '주스',
    '차',
    '커피',
    '포도',
    '홍합'
);

INSERT INTO ingredient_catalog (name, category, default_storage_type, emoji)
VALUES
    ('고추', 'VEGETABLE', 'FRIDGE', '🌶️'),
    ('고춧가루', 'SAUCE', 'PANTRY', '🌶️'),
    ('굴소스', 'SAUCE', 'FRIDGE', '🧂'),
    ('깻잎', 'VEGETABLE', 'FRIDGE', '🥬'),
    ('다시마', 'SEAFOOD', 'PANTRY', '🐟'),
    ('당면', 'GRAIN', 'PANTRY', '🍜'),
    ('라면사리', 'GRAIN', 'PANTRY', '🍜'),
    ('맥주', 'DRINK', 'FRIDGE', '🍺'),
    ('머스타드', 'SAUCE', 'FRIDGE', '🌭'),
    ('미역', 'SEAFOOD', 'PANTRY', '🐟'),
    ('배', 'FRUIT', 'FRIDGE', '🍐'),
    ('부추', 'VEGETABLE', 'FRIDGE', '🥬'),
    ('소주', 'DRINK', 'PANTRY', '🍶'),
    ('식초', 'SAUCE', 'PANTRY', '🧂'),
    ('쌈장', 'SAUCE', 'FRIDGE', '🫘'),
    ('옥수수', 'GRAIN', 'PANTRY', '🌽'),
    ('참기름', 'SAUCE', 'PANTRY', '🫙'),
    ('참깨', 'GRAIN', 'PANTRY', '🧂'),
    ('콜라', 'DRINK', 'FRIDGE', '🥤'),
    ('호박', 'VEGETABLE', 'FRIDGE', '🎃'),
    ('후추', 'ETC', 'PANTRY', '🧂')
ON CONFLICT (name) DO UPDATE SET
    category = EXCLUDED.category,
    default_storage_type = EXCLUDED.default_storage_type,
    emoji = EXCLUDED.emoji,
    updated_at = CURRENT_TIMESTAMP;

UPDATE ingredient_catalog
SET category = update_values.category,
    default_storage_type = update_values.default_storage_type,
    updated_at = CURRENT_TIMESTAMP
FROM (
    VALUES
        ('계란', 'ETC', 'FRIDGE'),
        ('김치', 'VEGETABLE', 'FRIDGE'),
        ('소금', 'ETC', 'PANTRY'),
        ('설탕', 'ETC', 'PANTRY'),
        ('식용유', 'ETC', 'PANTRY')
) AS update_values(name, category, default_storage_type)
WHERE ingredient_catalog.name = update_values.name;

SELECT setval(
    pg_get_serial_sequence('ingredient_catalog', 'id'),
    GREATEST((SELECT MAX(id) FROM ingredient_catalog), 1)
);
