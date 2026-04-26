-- V2__auth_schema.sql
-- Users and refresh tokens for Phase 5a authentication.

-- ─── User roles ──────────────────────────────────────────────────────────────
CREATE TYPE user_role AS ENUM ('VIEWER', 'EDITOR', 'ADMIN');

-- ─── Users ───────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            user_role    NOT NULL DEFAULT 'EDITOR',
    -- Reserved for future team/org features — nullable until Phase 5b teams
    organization_id UUID,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);

CREATE TRIGGER users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ─── Refresh tokens ──────────────────────────────────────────────────────────
-- Stored as hashed values — raw token is never persisted.
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Track where token was issued from for audit purposes
    issued_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id  ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_hash     ON refresh_tokens (token_hash);
-- Index for cleanup job (delete expired tokens)
CREATE INDEX idx_refresh_tokens_expires  ON refresh_tokens (expires_at);

-- ─── Add user ownership to templates ─────────────────────────────────────────
-- Nullable for now — existing templates (created before auth) are unowned.
-- Phase 5b will enforce non-null once all templates are migrated.
ALTER TABLE templates
    ADD COLUMN user_id UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_templates_user_id ON templates (user_id);