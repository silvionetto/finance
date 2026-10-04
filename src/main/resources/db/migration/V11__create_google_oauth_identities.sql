ALTER TABLE app_users
    ALTER COLUMN username TYPE VARCHAR(320);

CREATE TABLE app_user_oauth_identities (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_user_oauth_identity UNIQUE (provider, subject),
    CONSTRAINT uq_app_user_oauth_provider_per_user UNIQUE (user_id, provider),
    CONSTRAINT chk_app_user_oauth_provider CHECK (provider = 'google')
);
