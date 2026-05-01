CREATE TABLE IF NOT EXISTS ai_setting (
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT    NOT NULL UNIQUE,
    provide_extra_info     BOOLEAN   NOT NULL DEFAULT FALSE,
    priority_expiration    BOOLEAN   NOT NULL DEFAULT FALSE,
    priority_nutrition     BOOLEAN   NOT NULL DEFAULT FALSE,
    priority_frequent      BOOLEAN   NOT NULL DEFAULT FALSE,
    notify_recipe_complete BOOLEAN   NOT NULL DEFAULT FALSE,
    notify_ai_recommend    BOOLEAN   NOT NULL DEFAULT FALSE,
    response_style         VARCHAR(255),
    include_image          BOOLEAN   NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_ai_setting_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_setting_user_id ON ai_setting(user_id);