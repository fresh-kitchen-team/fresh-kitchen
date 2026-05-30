ALTER TABLE ingredient_catalog
    DROP CONSTRAINT ck_ingredient_catalog_category;

ALTER TABLE category_expiry_rule
    DROP CONSTRAINT ck_category_expiry_rule_category;

ALTER TABLE ingredient_catalog
    ADD CONSTRAINT ck_ingredient_catalog_category
        CHECK (category IN ('VEGETABLE', 'FRUIT', 'MEAT', 'SEAFOOD', 'DAIRY', 'SAUCE', 'DRINK', 'GRAIN', 'ETC'));

ALTER TABLE category_expiry_rule
    ADD CONSTRAINT ck_category_expiry_rule_category
        CHECK (category IN ('VEGETABLE', 'FRUIT', 'MEAT', 'SEAFOOD', 'DAIRY', 'SAUCE', 'DRINK', 'GRAIN', 'ETC'));

ALTER TABLE ingredient
    ADD COLUMN category VARCHAR(30);

UPDATE ingredient ingredient
SET category = catalog.category
FROM ingredient_catalog catalog
WHERE ingredient.catalog_id = catalog.id;

UPDATE ingredient
SET category = 'ETC'
WHERE category IS NULL;

ALTER TABLE ingredient
    ALTER COLUMN category SET NOT NULL;

ALTER TABLE ingredient
    ADD CONSTRAINT ck_ingredient_category
        CHECK (category IN ('VEGETABLE', 'FRUIT', 'MEAT', 'SEAFOOD', 'DAIRY', 'SAUCE', 'DRINK', 'GRAIN', 'ETC'));

UPDATE ingredient_catalog
SET category = 'GRAIN'
WHERE name IN ('밥', '면', '빵', '떡');

UPDATE ingredient ingredient
SET category = 'GRAIN'
FROM ingredient_catalog catalog
WHERE ingredient.catalog_id = catalog.id
  AND catalog.name IN ('밥', '면', '빵', '떡');

INSERT INTO category_expiry_rule (category, storage_type, shelf_life_days, reference_note)
VALUES
    ('GRAIN', 'FRIDGE', 5, 'grain fridge fallback'),
    ('GRAIN', 'FREEZER', 30, 'grain freezer fallback'),
    ('GRAIN', 'PANTRY', 14, 'grain pantry fallback')
ON CONFLICT (category, storage_type) DO UPDATE SET
    shelf_life_days = EXCLUDED.shelf_life_days,
    reference_note = EXCLUDED.reference_note,
    updated_at = CURRENT_TIMESTAMP;
