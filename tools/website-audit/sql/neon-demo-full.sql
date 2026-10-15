-- Fixna Website Audit full demo dataset for Neon.
-- Run after the Website Audit Flyway migrations are applied.
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
    SELECT 'Fixna Website Audit Demo', 'SMB'
    WHERE NOT EXISTS (
        SELECT 1 FROM tenants WHERE name = 'Fixna Website Audit Demo'
    )
    RETURNING id
),
tenant_seed AS (
    SELECT COALESCE(
        (SELECT id FROM tenants WHERE name = 'Fixna Website Audit Demo'),
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
new_audit AS (
    INSERT INTO website_audits (
        tenant_id, input_url, normalized_url_hash, status, overall_score,
        summary, created_by_user_id
    )
    SELECT t.tenant_id,
           'https://demo-cafe.example.com',
           encode(digest('https://demo-cafe.example.com', 'sha256'), 'hex'),
           'COMPLETED',
           87,
           'Demo website audit showing good SEO fundamentals with room for performance improvements.',
           u.id
    FROM tenant_seed t
    CROSS JOIN users u
    WHERE u.email = 'owner@example.com'
      AND NOT EXISTS (
          SELECT 1
          FROM website_audits
          WHERE tenant_id = t.tenant_id
            AND input_url = 'https://demo-cafe.example.com'
      )
    RETURNING id, tenant_id
),
new_checks AS (
    INSERT INTO website_audit_checks (tenant_id, audit_id, check_code, severity, score, evidence, fix_hint)
    SELECT a.tenant_id, a.id, 'HTTPS_ENFORCED', 'INFO', 100,
           '{"enabled": true}'::jsonb, 'Already enforced'
    FROM new_audit a
    WHERE NOT EXISTS (
        SELECT 1 FROM website_audit_checks WHERE audit_id = a.id AND check_code = 'HTTPS_ENFORCED'
    )
),
new_checks2 AS (
    INSERT INTO website_audit_checks (tenant_id, audit_id, check_code, severity, score, evidence, fix_hint)
    SELECT a.tenant_id, a.id, 'TITLE_TAG_PRESENT', 'INFO', 90,
           '{"title": "Demo Neighbourhood Cafe - Great Coffee"}'::jsonb, 'Title is well optimized'
    FROM new_audit a
    WHERE NOT EXISTS (
        SELECT 1 FROM website_audit_checks WHERE audit_id = a.id AND check_code = 'TITLE_TAG_PRESENT'
    )
),
new_checks3 AS (
    INSERT INTO website_audit_checks (tenant_id, audit_id, check_code, severity, score, evidence, fix_hint)
    SELECT a.tenant_id, a.id, 'META_DESCRIPTION_PRESENT', 'WARNING', 60,
           '{"description": "Great coffee in your neighbourhood"}'::jsonb, 'Description could be longer (150-160 chars)'
    FROM new_audit a
    WHERE NOT EXISTS (
        SELECT 1 FROM website_audit_checks WHERE audit_id = a.id AND check_code = 'META_DESCRIPTION_PRESENT'
    )
),
new_checks4 AS (
    INSERT INTO website_audit_checks (tenant_id, audit_id, check_code, severity, score, evidence, fix_hint)
    SELECT a.tenant_id, a.id, 'MOBILE_FRIENDLY', 'INFO', 95,
           '{"viewport": "width=device-width, initial-scale=1"}'::jsonb, 'Mobile viewport configured correctly'
    FROM new_audit a
    WHERE NOT EXISTS (
        SELECT 1 FROM website_audit_checks WHERE audit_id = a.id AND check_code = 'MOBILE_FRIENDLY'
    )
),
new_checks5 AS (
    INSERT INTO website_audit_checks (tenant_id, audit_id, check_code, severity, score, evidence, fix_hint)
    SELECT a.tenant_id, a.id, 'PAGE_LOAD_SPEED', 'ERROR', 45,
           '{"load_time_ms": 4200}'::jsonb, 'Optimize images and enable compression'
    FROM new_audit a
    WHERE NOT EXISTS (
        SELECT 1 FROM website_audit_checks WHERE audit_id = a.id AND check_code = 'PAGE_LOAD_SPEED'
    )
),
new_checks6 AS (
    INSERT INTO website_audit_checks (tenant_id, audit_id, check_code, severity, score, evidence, fix_hint)
    SELECT a.tenant_id, a.id, 'STRUCTURED_DATA', 'WARNING', 70,
           '{"has_schema": true, "types": ["LocalBusiness"]}'::jsonb, 'Add more schema types like Menu, OpeningHours'
    FROM new_audit a
    WHERE NOT EXISTS (
        SELECT 1 FROM website_audit_checks WHERE audit_id = a.id AND check_code = 'STRUCTURED_DATA'
    )
),
new_job AS (
    INSERT INTO website_audit_jobs (
        tenant_id, audit_id, requested_url, status, crawl_enabled,
        attempt, created_by_user_id
    )
    SELECT a.tenant_id, a.id, 'https://demo-cafe.example.com', 'COMPLETED', true,
           0, u.id
    FROM new_audit a
    CROSS JOIN users u
    WHERE u.email = 'owner@example.com'
      AND NOT EXISTS (
          SELECT 1
          FROM website_audit_jobs
          WHERE tenant_id = a.tenant_id
            AND requested_url = 'https://demo-cafe.example.com'
      )
    RETURNING id, tenant_id, audit_id
),
new_pages AS (
    INSERT INTO website_audit_pages (tenant_id, audit_id, job_id, url, url_hash, depth, http_status, content_type, size_bytes, fetch_ms)
    SELECT j.tenant_id, j.audit_id, j.id, p.url, encode(digest(p.url, 'sha256'), 'hex'), p.depth, p.http_status, p.content_type, p.size_bytes, p.fetch_ms
    FROM new_job j
    CROSS JOIN LATERAL (VALUES
        ('https://demo-cafe.example.com/', 0, 200, 'text/html', 24500, 320),
        ('https://demo-cafe.example.com/menu', 1, 200, 'text/html', 18200, 280),
        ('https://demo-cafe.example.com/about', 1, 200, 'text/html', 15600, 250),
        ('https://demo-cafe.example.com/contact', 1, 200, 'text/html', 12300, 210),
        ('https://demo-cafe.example.com/blog/coffee-tips', 2, 200, 'text/html', 31000, 410)
    ) AS p(url, depth, http_status, content_type, size_bytes, fetch_ms)
    WHERE NOT EXISTS (
        SELECT 1
        FROM website_audit_pages
        WHERE audit_id = j.audit_id
          AND url = p.url
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
    'tenant', 'Fixna Website Audit Demo',
    'user', 'owner@example.com',
    'audit', 'https://demo-cafe.example.com',
    'result', 'demo data ready'
) AS result;