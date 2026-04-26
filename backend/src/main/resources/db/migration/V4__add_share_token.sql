-- V4__add_share_token.sql
-- Adds share token support to templates.
--
-- Design decisions:
--   - UUID type: unguessable, no sequential enumeration risk
--   - NULL by default: sharing is opt-in, existing templates are unaffected
--   - UNIQUE constraint: ensures no two templates share the same public URL
--   - No expiry column: tokens are permanent until revoked (Phase 5b decision).
--     If expiry is added in the future, add expires_at TIMESTAMP WITH TIME ZONE NULL
--     and a scheduled cleanup job — no schema change to this column needed.

ALTER TABLE templates
    ADD COLUMN share_token UUID NULL;

CREATE UNIQUE INDEX idx_templates_share_token
    ON templates (share_token)
    WHERE share_token IS NOT NULL;