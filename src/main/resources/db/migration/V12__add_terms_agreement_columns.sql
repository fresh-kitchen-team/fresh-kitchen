ALTER TABLE users ADD COLUMN terms_agreed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN privacy_agreed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE users ADD CONSTRAINT chk_terms_agreement_pair
    CHECK (
        (terms_agreed_at IS NULL AND privacy_agreed_at IS NULL)
        OR (terms_agreed_at IS NOT NULL AND privacy_agreed_at IS NOT NULL)
    );
