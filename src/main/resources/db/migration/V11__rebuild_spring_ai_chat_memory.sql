-- Replace legacy spring_ai_chat_memory table (created in V5) with the Spring AI 1.1.6
-- standard schema expected by JdbcChatMemoryRepository.
-- Old schema used `id`/`created_at`; new repository queries `"timestamp"` (quoted).
DROP TABLE IF EXISTS spring_ai_chat_memory;

CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
    conversation_id VARCHAR(36) NOT NULL,
    content         TEXT        NOT NULL,
    type            VARCHAR(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp"     TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS spring_ai_chat_memory_conversation_id_timestamp_idx
    ON SPRING_AI_CHAT_MEMORY (conversation_id, "timestamp");
