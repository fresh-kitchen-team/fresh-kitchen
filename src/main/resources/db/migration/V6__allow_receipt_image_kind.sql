ALTER TABLE image_asset
    DROP CONSTRAINT ck_image_asset_kind;

ALTER TABLE image_asset
    ADD CONSTRAINT ck_image_asset_kind
        CHECK (kind IN ('INGREDIENT', 'RECEIPT'));
