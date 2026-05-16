-- Migrate vector_store to gemini-embedding-001 (3072 dimensions)
-- Existing 768-dim embeddings (text-embedding-004) are incompatible with the new model,
-- so the table must be truncated before altering the column type.
TRUNCATE TABLE vector_store;

ALTER TABLE vector_store
    ALTER COLUMN embedding TYPE VECTOR(3072);
