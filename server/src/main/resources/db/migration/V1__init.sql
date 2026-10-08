-- ConnectHub V1: identity, interests, refresh tokens.
-- Every table: id UUID (gen_random_uuid()), created_at / updated_at TIMESTAMPTZ.

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TYPE auth_provider AS ENUM ('local', 'google');

CREATE TABLE users (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email              VARCHAR(254) NOT NULL,
    password_hash      VARCHAR(100),                 -- null for OAuth-only accounts
    provider           auth_provider NOT NULL DEFAULT 'local',
    provider_id        VARCHAR(255),
    username           VARCHAR(30),                  -- null until onboarding picks one
    display_name       VARCHAR(60),
    bio                VARCHAR(160),
    avatar_url         VARCHAR(512),
    cover_url          VARCHAR(512),
    location           VARCHAR(80),
    website            VARCHAR(255),
    is_private         BOOLEAN NOT NULL DEFAULT FALSE,
    onboarded          BOOLEAN NOT NULL DEFAULT FALSE,
    notification_prefs JSONB NOT NULL DEFAULT '{"reactions":true,"comments":true,"replies":true,"circles":true,"messages":true}',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT users_username_format CHECK (username IS NULL OR username ~ '^[a-z0-9_]{3,30}$')
);
CREATE UNIQUE INDEX users_email_uk ON users (lower(email));
CREATE UNIQUE INDEX users_username_uk ON users (username) WHERE username IS NOT NULL;
CREATE UNIQUE INDEX users_provider_uk ON users (provider, provider_id) WHERE provider_id IS NOT NULL;
CREATE TRIGGER users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE interests (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug       VARCHAR(40) NOT NULL UNIQUE,
    name       VARCHAR(60) NOT NULL,
    emoji      VARCHAR(8),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER interests_updated_at BEFORE UPDATE ON interests FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE user_interests (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    interest_id UUID NOT NULL REFERENCES interests (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT user_interests_uk UNIQUE (user_id, interest_id)
);
CREATE INDEX user_interests_user_idx ON user_interests (user_id);
CREATE INDEX user_interests_interest_idx ON user_interests (interest_id);
CREATE TRIGGER user_interests_updated_at BEFORE UPDATE ON user_interests FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,         -- SHA-256 hex; raw token only lives in the cookie
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    replaced_by UUID REFERENCES refresh_tokens (id) ON DELETE SET NULL,
    user_agent  VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX refresh_tokens_user_idx ON refresh_tokens (user_id);
CREATE INDEX refresh_tokens_replaced_by_idx ON refresh_tokens (replaced_by);
CREATE INDEX refresh_tokens_expires_idx ON refresh_tokens (expires_at);
CREATE TRIGGER refresh_tokens_updated_at BEFORE UPDATE ON refresh_tokens FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Interests are reference data needed in every environment (onboarding), not just dev.
INSERT INTO interests (slug, name, emoji) VALUES
    ('photography', 'Photography', '📷'),
    ('music', 'Music', '🎧'),
    ('books', 'Books', '📚'),
    ('film', 'Film', '🎬'),
    ('cooking', 'Cooking', '🍳'),
    ('travel', 'Travel', '🧭'),
    ('fitness', 'Fitness', '🏃'),
    ('gaming', 'Gaming', '🎮'),
    ('design', 'Design', '🎨'),
    ('tech', 'Tech', '💻'),
    ('science', 'Science', '🔭'),
    ('nature', 'Nature', '🌿'),
    ('fashion', 'Fashion', '🧵'),
    ('art', 'Art', '🖌️'),
    ('writing', 'Writing', '✍️'),
    ('mental-health', 'Mental health', '🫶'),
    ('startups', 'Startups', '🚀'),
    ('pets', 'Pets', '🐾'),
    ('sports', 'Sports', '⚽'),
    ('coffee', 'Coffee', '☕');
