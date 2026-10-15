-- Fixna Hospitality full demo dataset for Neon.
-- Run after the Hospitality Flyway migrations are applied.
-- This script is idempotent and safe to run repeatedly in the Neon SQL Editor.

BEGIN;

-- Demo password: replace the hash below with a BCrypt hash for a login you control.
-- The current placeholder is intentionally not a valid password and must be replaced.
WITH password_seed AS (
    SELECT '$2a$10$BAFhp4UGC7YVYh9WjPHndeYV7CnoADpM2Q2IpS6.CJ/xnjsfBE1K2'::VARCHAR(255) AS password_hash
),
-- Use consistent user IDs across all 5 products (same email = same user)
shared_users AS (
    INSERT INTO users (id, email, password_hash, first_name, last_name)
    VALUES
        (gen_random_uuid(), 'owner@example.com', (SELECT password_hash FROM password_seed), 'Demo', 'Owner'),
        (gen_random_uuid(), 'consultant@example.com', (SELECT password_hash FROM password_seed), 'Demo', 'Consultant'),
        (gen_random_uuid(), 'client@example.com', (SELECT password_hash FROM password_seed), 'Portal', 'User')
    ON CONFLICT (email) DO UPDATE SET
        password_hash = EXCLUDED.password_hash,
        first_name = EXCLUDED.first_name,
        last_name = EXCLUDED.last_name
    RETURNING id, email
),
new_tenant AS (
    INSERT INTO tenants (name, tenant_type)
    SELECT 'Fixna Hospitality Demo', 'HOSPITALITY_WORKSPACE'
    WHERE NOT EXISTS (
        SELECT 1 FROM tenants WHERE name = 'Fixna Hospitality Demo'
    )
    RETURNING id
),
tenant_seed AS (
    SELECT COALESCE(
        (SELECT id FROM tenants WHERE name = 'Fixna Hospitality Demo'),
        (SELECT id FROM new_tenant)
    ) AS tenant_id
),
new_membership AS (
    INSERT INTO tenant_memberships (tenant_id, user_id, role)
    SELECT t.tenant_id, u.id, 'TENANT_OWNER'
    FROM tenant_seed t
    CROSS JOIN users u
    WHERE u.email = 'owner@example.com'
      AND NOT EXISTS (
          SELECT 1
          FROM tenant_memberships tm
          WHERE tm.tenant_id = t.tenant_id
            AND tm.user_id = u.id
      )
),
new_audit_log AS (
    INSERT INTO audit_logs (
        tenant_id, user_id, action, resource_type, resource_id, metadata
    )
    SELECT t.tenant_id, u.id, 'DEMO_SEED', 'TENANT', t.tenant_id,
           '{"source":"neon-demo-full"}'::jsonb
    FROM tenant_seed t
    CROSS JOIN users u
    WHERE u.email = 'owner@example.com'
      AND NOT EXISTS (
          SELECT 1
          FROM audit_logs
          WHERE tenant_id = t.tenant_id
            AND user_id = u.id
            AND action = 'DEMO_SEED'
      )
),
new_refresh_token AS (
    INSERT INTO refresh_tokens (
        user_id, tenant_id, token_hash, expires_at
    )
    SELECT u.id, t.tenant_id,
           encode(digest('demo-refresh-token', 'sha256'), 'hex'),
           now() + interval '30 days'
    FROM users u
    CROSS JOIN tenant_seed t
    WHERE u.email = 'owner@example.com'
      AND NOT EXISTS (
          SELECT 1
          FROM refresh_tokens
          WHERE user_id = u.id
            AND tenant_id = t.tenant_id
      )
)
SELECT 1;

COMMIT;

SELECT json_build_object(
    'tenant', 'Fixna Hospitality Demo',
    'user', 'owner@example.com',
    'status', 'platform core only - hospitality tables added via Flyway',
    'result', 'demo data ready'
) AS result;