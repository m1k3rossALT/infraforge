-- V5__add_ai_settings.sql
-- Adds BYOK (Bring Your Own Key) AI configuration to users.
--
-- Design decisions:
--   - All columns nullable: AI is opt-in, existing users are unaffected
--   - api key is stored encrypted (AES-256-GCM) — plaintext key is never persisted
--   - ai_model is nullable: each provider has a sensible default in code
--   - No separate ai_settings table: one-to-one with user, embedded is simpler at this scale
--
-- Future (Phase 6 — Subscription & Managed AI):
--   - Add subscription_status column here to gate platform-managed AI
--   - BYOK columns remain so users can still use their own key on any tier

ALTER TABLE users
    ADD COLUMN ai_provider     VARCHAR(32)  NULL,
    ADD COLUMN ai_api_key_enc  TEXT         NULL,
    ADD COLUMN ai_model        VARCHAR(64)  NULL;

COMMENT ON COLUMN users.ai_provider    IS 'AI provider identifier: gemini, openai, anthropic, mistral, groq';
COMMENT ON COLUMN users.ai_api_key_enc IS 'AES-256-GCM encrypted API key — IV prepended, Base64-encoded';
COMMENT ON COLUMN users.ai_model       IS 'Optional model override. NULL = use provider default in code';