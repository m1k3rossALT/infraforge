-- V1__baseline_schema.sql
-- InfraForge baseline database schema
-- Flyway applies this automatically on first startup.

-- ─── Templates ───────────────────────────────────────────────────────────────
-- Stores user-saved IaC templates.
-- form_state is stored as JSONB — flexible, queryable, and extensible.
-- Adding new fields to the form in the future does not require a schema change.

CREATE TABLE templates (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255)  NOT NULL,
    provider_id   VARCHAR(64)   NOT NULL,
    form_state    JSONB         NOT NULL,
    generated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    description   TEXT,
    tags          TEXT[]
);

-- Index for listing templates by provider
CREATE INDEX idx_templates_provider_id ON templates (provider_id);

-- Index for full-text search on name (future use)
CREATE INDEX idx_templates_name ON templates (name);

-- Index on tags array for tag-based filtering (future use)
CREATE INDEX idx_templates_tags ON templates USING GIN (tags);

-- Index on form_state JSONB for querying specific field values (future use)
CREATE INDEX idx_templates_form_state ON templates USING GIN (form_state);

-- ─── Auto-update updated_at ──────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER templates_updated_at
    BEFORE UPDATE ON templates
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();