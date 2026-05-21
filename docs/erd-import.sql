-- Fresh-Kitchen Final Schema (V1 ~ V14)
-- ERDCloud import용 DDL

CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    provider_user_id VARCHAR(255) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    inactive_at TIMESTAMPTZ,
    terms_agreed_at TIMESTAMPTZ,
    privacy_agreed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE user_profile (
    user_id BIGINT PRIMARY KEY,
    nickname VARCHAR(100) NOT NULL,
    profile_image_url TEXT,
    bio TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_profile_preferred_ingredient (
    user_id BIGINT NOT NULL,
    ingredient_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, ingredient_name),
    FOREIGN KEY (user_id) REFERENCES user_profile(user_id)
);

CREATE TABLE user_profile_food_style (
    user_id BIGINT NOT NULL,
    food_style VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, food_style),
    FOREIGN KEY (user_id) REFERENCES user_profile(user_id)
);

CREATE TABLE user_profile_allergy (
    user_id BIGINT NOT NULL,
    allergy_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, allergy_type),
    FOREIGN KEY (user_id) REFERENCES user_profile(user_id)
);

CREATE TABLE user_profile_cooking_tool (
    user_id BIGINT NOT NULL,
    cooking_tool VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, cooking_tool),
    FOREIGN KEY (user_id) REFERENCES user_profile(user_id)
);

CREATE TABLE storage (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    storage_type VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE image_asset (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    asset_type VARCHAR(30) NOT NULL,
    kind VARCHAR(30) NOT NULL,
    storage_provider VARCHAR(20) NOT NULL,
    object_key TEXT NOT NULL,
    width INT,
    height INT,
    created_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE image_variant (
    id BIGINT PRIMARY KEY,
    image_asset_id BIGINT NOT NULL,
    variant_type VARCHAR(20) NOT NULL,
    object_key TEXT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (image_asset_id) REFERENCES image_asset(id)
);

CREATE TABLE ingredient_catalog (
    id BIGINT PRIMARY KEY,
    default_image_asset_id BIGINT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(30) NOT NULL,
    default_storage_type VARCHAR(20) NOT NULL,
    emoji TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (default_image_asset_id) REFERENCES image_asset(id)
);

CREATE TABLE category_expiry_rule (
    id BIGINT PRIMARY KEY,
    category VARCHAR(30) NOT NULL,
    storage_type VARCHAR(20) NOT NULL,
    shelf_life_days INT NOT NULL,
    reference_note TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE catalog_expiry_rule (
    id BIGINT PRIMARY KEY,
    catalog_id BIGINT NOT NULL,
    storage_type VARCHAR(20) NOT NULL,
    shelf_life_days INT NOT NULL,
    reference_note TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (catalog_id) REFERENCES ingredient_catalog(id)
);

CREATE TABLE ingredient (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    storage_id BIGINT NOT NULL,
    catalog_id BIGINT,
    name VARCHAR(100) NOT NULL,
    registered_at DATE,
    expires_at DATE,
    expiry_source_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    consumed_at DATE,
    discarded_at DATE,
    note TEXT,
    source_type VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (storage_id) REFERENCES storage(id),
    FOREIGN KEY (catalog_id) REFERENCES ingredient_catalog(id)
);

CREATE TABLE ingredient_image (
    id BIGINT PRIMARY KEY,
    ingredient_id BIGINT NOT NULL,
    image_asset_id BIGINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    source_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (ingredient_id) REFERENCES ingredient(id),
    FOREIGN KEY (image_asset_id) REFERENCES image_asset(id)
);

CREATE TABLE chat_room (
    id BIGINT PRIMARY KEY,
    title VARCHAR(255),
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE chat_message (
    id BIGINT PRIMARY KEY,
    content TEXT NOT NULL,
    sender VARCHAR(50) NOT NULL,
    ai_payload TEXT,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (room_id) REFERENCES chat_room(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE ai_setting (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    provide_extra_info BOOLEAN NOT NULL DEFAULT FALSE,
    priority_expiration BOOLEAN NOT NULL DEFAULT FALSE,
    priority_nutrition BOOLEAN NOT NULL DEFAULT FALSE,
    priority_frequent BOOLEAN NOT NULL DEFAULT FALSE,
    notify_recipe_complete BOOLEAN NOT NULL DEFAULT FALSE,
    notify_ai_recommend BOOLEAN NOT NULL DEFAULT FALSE,
    response_style VARCHAR(255),
    include_image BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE vector_store (
    id UUID PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding VARCHAR(50)
);

CREATE TABLE spring_ai_chat_memory (
    conversation_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(10) NOT NULL,
    timestamp TIMESTAMP NOT NULL
);

CREATE TABLE storage_tip (
    id BIGINT PRIMARY KEY,
    category VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    emoji VARCHAR(10),
    tip TEXT NOT NULL,
    storage_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE recycling_guide (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    waste_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
