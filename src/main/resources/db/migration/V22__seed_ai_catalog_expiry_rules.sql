UPDATE ingredient_catalog
SET category = update_values.category,
    default_storage_type = update_values.default_storage_type,
    updated_at = CURRENT_TIMESTAMP
FROM (
    VALUES
        ('옥수수', 'VEGETABLE', 'FRIDGE'),
        ('참치', 'SEAFOOD', 'PANTRY')
) AS update_values(name, category, default_storage_type)
WHERE ingredient_catalog.name = update_values.name;

INSERT INTO catalog_expiry_rule (catalog_id, storage_type, shelf_life_days, reference_note)
SELECT catalog.id, rule.storage_type::VARCHAR, rule.shelf_life_days, rule.reference_note
FROM ingredient_catalog catalog
JOIN (
    VALUES
        ('멸치', 'PANTRY', 180, 'dried anchovy pantry policy'),
        ('사과', 'FRIDGE', 30, 'apple fridge policy'),
        ('베이컨', 'FRIDGE', 7, 'bacon fridge policy'),
        ('바나나', 'PANTRY', 5, 'banana pantry policy'),
        ('콩나물', 'FRIDGE', 4, 'bean sprout fridge policy'),
        ('소고기', 'FRIDGE', 3, 'beef fridge policy'),
        ('맥주', 'FRIDGE', 180, 'beer fridge policy'),
        ('파프리카', 'FRIDGE', 7, 'paprika fridge policy'),
        ('블루베리', 'FRIDGE', 7, 'blueberry fridge policy'),
        ('브로콜리', 'FRIDGE', 5, 'broccoli fridge policy'),
        ('버터', 'FRIDGE', 30, 'butter fridge policy'),
        ('양배추', 'FRIDGE', 14, 'cabbage fridge policy'),
        ('당근', 'FRIDGE', 21, 'carrot fridge policy'),
        ('치즈', 'FRIDGE', 30, 'cheese fridge policy'),
        ('닭고기', 'FRIDGE', 2, 'chicken fridge policy'),
        ('고추', 'FRIDGE', 7, 'chili pepper fridge policy'),
        ('고춧가루', 'PANTRY', 365, 'chili powder pantry policy'),
        ('부추', 'FRIDGE', 5, 'chive fridge policy'),
        ('콜라', 'FRIDGE', 180, 'cola fridge policy'),
        ('식용유', 'PANTRY', 365, 'cooking oil pantry policy'),
        ('옥수수', 'FRIDGE', 5, 'corn fridge policy'),
        ('오이', 'FRIDGE', 7, 'cucumber fridge policy'),
        ('된장', 'FRIDGE', 180, 'doenjang fridge policy'),
        ('계란', 'FRIDGE', 21, 'egg fridge policy'),
        ('가지', 'FRIDGE', 5, 'eggplant fridge policy'),
        ('마늘', 'PANTRY', 30, 'garlic pantry policy'),
        ('당면', 'PANTRY', 365, 'glass noodle pantry policy'),
        ('고추장', 'FRIDGE', 180, 'gochujang fridge policy'),
        ('대파', 'FRIDGE', 7, 'green onion fridge policy'),
        ('다시마', 'PANTRY', 365, 'kelp pantry policy'),
        ('케첩', 'FRIDGE', 180, 'ketchup fridge policy'),
        ('김치', 'FRIDGE', 30, 'kimchi fridge policy'),
        ('키위', 'FRIDGE', 5, 'kiwi fridge policy'),
        ('상추', 'FRIDGE', 5, 'lettuce fridge policy'),
        ('고등어', 'FRIDGE', 2, 'mackerel fridge policy'),
        ('마요네즈', 'FRIDGE', 90, 'mayonnaise fridge policy'),
        ('우유', 'FRIDGE', 7, 'milk fridge policy'),
        ('버섯', 'FRIDGE', 5, 'mushroom fridge policy'),
        ('머스타드', 'FRIDGE', 180, 'mustard fridge policy'),
        ('양파', 'PANTRY', 30, 'onion pantry policy'),
        ('오렌지', 'FRIDGE', 14, 'orange fridge policy'),
        ('굴소스', 'FRIDGE', 90, 'oyster sauce fridge policy'),
        ('배', 'FRIDGE', 14, 'pear fridge policy'),
        ('후추', 'PANTRY', 365, 'black pepper pantry policy'),
        ('깻잎', 'FRIDGE', 5, 'perilla leaf fridge policy'),
        ('돼지고기', 'FRIDGE', 3, 'pork fridge policy'),
        ('감자', 'PANTRY', 30, 'potato pantry policy'),
        ('호박', 'FRIDGE', 7, 'pumpkin fridge policy'),
        ('무', 'FRIDGE', 14, 'radish fridge policy'),
        ('라면사리', 'PANTRY', 180, 'ramen noodle pantry policy'),
        ('떡', 'FRIDGE', 7, 'rice cake fridge policy'),
        ('소금', 'PANTRY', 365, 'salt pantry policy'),
        ('햄', 'FRIDGE', 7, 'ham fridge policy'),
        ('미역', 'PANTRY', 365, 'seaweed pantry policy'),
        ('참깨', 'PANTRY', 365, 'sesame pantry policy'),
        ('참기름', 'PANTRY', 180, 'sesame oil pantry policy'),
        ('새우', 'FREEZER', 60, 'shrimp freezer policy'),
        ('소주', 'PANTRY', 365, 'soju pantry policy'),
        ('간장', 'PANTRY', 365, 'soy sauce pantry policy'),
        ('시금치', 'FRIDGE', 5, 'spinach fridge policy'),
        ('오징어', 'FREEZER', 60, 'squid freezer policy'),
        ('쌈장', 'FRIDGE', 180, 'ssamjang fridge policy'),
        ('딸기', 'FRIDGE', 3, 'strawberry fridge policy'),
        ('설탕', 'PANTRY', 365, 'sugar pantry policy'),
        ('고구마', 'PANTRY', 30, 'sweet potato pantry policy'),
        ('두부', 'FRIDGE', 5, 'tofu fridge policy'),
        ('토마토', 'FRIDGE', 7, 'tomato fridge policy'),
        ('참치', 'PANTRY', 365, 'canned tuna pantry policy'),
        ('식초', 'PANTRY', 365, 'vinegar pantry policy'),
        ('애호박', 'FRIDGE', 7, 'zucchini fridge policy')
) AS rule(name, storage_type, shelf_life_days, reference_note)
ON catalog.name = rule.name
ON CONFLICT (catalog_id, storage_type) DO UPDATE SET
    shelf_life_days = EXCLUDED.shelf_life_days,
    reference_note = EXCLUDED.reference_note,
    updated_at = CURRENT_TIMESTAMP;
