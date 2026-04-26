-- V3__fix_user_role_column.sql
-- PostgreSQL native ENUM types are incompatible with JPA @Enumerated(STRING)
-- via JDBC prepared statements. The driver sends the value as character varying
-- and PostgreSQL refuses the implicit cast, causing registration to fail with:
--   ERROR: column "role" is of type user_role but expression is of type character varying
--
-- The column default ('EDITOR'::user_role) depends on the type, so we must
-- remove it before altering the column and before dropping the type.

-- Step 1: Remove the default that holds a reference to the enum type
ALTER TABLE users ALTER COLUMN role DROP DEFAULT;

-- Step 2: Convert the column to VARCHAR(50) — existing 'VIEWER'/'EDITOR'/'ADMIN'
--         string values are preserved as-is by the implicit cast
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(50);

-- Step 3: Re-add the default as a plain string (no longer tied to the enum type)
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'EDITOR';

-- Step 4: Type now has no dependents — safe to drop
DROP TYPE IF EXISTS user_role;