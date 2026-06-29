ALTER TABLE image_asset
    RENAME CONSTRAINT ck_image_asset_image_url_not_blank TO ck_image_asset_object_key_not_blank;

ALTER TABLE image_asset
    RENAME COLUMN image_url TO object_key;

ALTER TABLE image_variant
    RENAME CONSTRAINT ck_image_variant_image_url_not_blank TO ck_image_variant_object_key_not_blank;

ALTER TABLE image_variant
    RENAME COLUMN image_url TO object_key;
