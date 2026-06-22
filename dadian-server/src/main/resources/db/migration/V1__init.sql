CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone         VARCHAR(20) UNIQUE NOT NULL,
    phone_hash    VARCHAR(64) UNIQUE,
    phone_encrypted BYTEA,
    display_name  VARCHAR(40) NOT NULL,
    avatar_url    TEXT,
    bio           VARCHAR(120),
    social_trait      SMALLINT CHECK (social_trait BETWEEN 1 AND 3),
    weekend_style     SMALLINT CHECK (weekend_style BETWEEN 1 AND 3),
    crowd_feeling     SMALLINT CHECK (crowd_feeling BETWEEN 1 AND 3),
    companion_tone       VARCHAR(12) DEFAULT 'gentle',
    companion_intensity  SMALLINT DEFAULT 50,
    humor_level          SMALLINT DEFAULT 30,
    achievement_visibility VARCHAR(10) DEFAULT 'private',
    location_retention     VARCHAR(20) DEFAULT '7 days',
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ DEFAULT now(),
    updated_at    TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
