CREATE OR REPLACE FUNCTION enforce_ingredient_image_primary_invariant()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    target_ingredient_id BIGINT;
    image_count BIGINT;
    primary_count BIGINT;
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.ingredient_id IS DISTINCT FROM OLD.ingredient_id THEN
        RAISE EXCEPTION 'ingredient_image.ingredient_id must not change';
    END IF;

    target_ingredient_id := COALESCE(NEW.ingredient_id, OLD.ingredient_id);

    SELECT COUNT(*), COUNT(*) FILTER (WHERE is_primary = TRUE)
      INTO image_count, primary_count
      FROM ingredient_image
     WHERE ingredient_id = target_ingredient_id;

    IF image_count > 0 AND primary_count <> 1 THEN
        RAISE EXCEPTION 'ingredient % must have exactly one primary image', target_ingredient_id;
    END IF;

    RETURN NULL;
END;
$$;
