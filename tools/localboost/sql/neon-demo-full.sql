-- Fixna LocalBoost full demo dataset for Neon.
-- Run after the LocalBoost Flyway migrations are applied.
-- This script is idempotent and safe to run repeatedly in the Neon SQL Editor.

BEGIN;

-- Demo password: replace the hash below with a BCrypt hash for a login you control.
-- The current placeholder is intentionally not a valid password and must be replaced.
WITH password_seed AS (
    SELECT '$2a$10$BAFhp4UGC7YVYh9WjPHndeYV7CnoADpM2Q2IpS6.CJ/xnjsfBE1K2'::VARCHAR(255) AS password_hash
),
new_user AS (
    INSERT INTO users (email, password_hash, first_name, last_name)
    SELECT 'owner@example.com', password_hash, 'Demo', 'Owner'
    FROM password_seed
    WHERE NOT EXISTS (
        SELECT 1 FROM users WHERE email = 'owner@example.com'
    )
    RETURNING id
),
new_tenant AS (
    INSERT INTO tenants (name, tenant_type)
    SELECT 'Fixna Demo Workspace', 'SMB'
    WHERE NOT EXISTS (
        SELECT 1 FROM tenants WHERE name = 'Fixna Demo Workspace'
    )
    RETURNING id
),
tenant_seed AS (
    SELECT COALESCE(
        (SELECT id FROM tenants WHERE name = 'Fixna Demo Workspace'),
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
new_business AS (
    INSERT INTO businesses (tenant_id, name, category, description, website_url, phone)
    SELECT t.tenant_id,
           'Demo Neighbourhood Cafe',
           'CAFE',
           'Sample business covering the complete LocalBoost journey.',
           'https://example.com/demo-cafe',
           '+91 98765 43210'
    FROM tenant_seed t
    WHERE NOT EXISTS (
        SELECT 1
        FROM businesses
        WHERE tenant_id = t.tenant_id
          AND name = 'Demo Neighbourhood Cafe'
    )
    RETURNING id, tenant_id
),
new_location AS (
    INSERT INTO business_locations (
        tenant_id, business_id, address_line, city, state, postal_code, country
    )
    SELECT b.tenant_id, b.id,
           'Demo Street 12, Sector 18', 'Noida', 'Uttar Pradesh', '201301', 'India'
    FROM new_business b
    WHERE NOT EXISTS (
        SELECT 1
        FROM business_locations
        WHERE business_id = b.id
    )
),
new_subscription AS (
    INSERT INTO subscriptions (tenant_id, plan_code, status, starts_at)
    SELECT t.tenant_id, 'STARTER', 'ACTIVE', now()
    FROM tenant_seed t
    WHERE NOT EXISTS (
        SELECT 1 FROM subscriptions WHERE tenant_id = t.tenant_id
    )
),
new_campaign AS (
    INSERT INTO campaigns (
        tenant_id, business_id, name, objective, status, total_budget, currency,
        start_at, end_at
    )
    SELECT b.tenant_id, b.id,
           'DEMO - Cafe opening results',
           'LEAD_GENERATION',
           'COMPLETED',
           7000.00,
           'INR',
           now() - interval '8 days',
           now() - interval '1 day'
    FROM new_business b
    WHERE NOT EXISTS (
        SELECT 1
        FROM campaigns c
        WHERE c.business_id = b.id
          AND c.name = 'DEMO - Cafe opening results'
    )
    RETURNING id, tenant_id, business_id, total_budget
),
new_offer AS (
    INSERT INTO campaign_offers (tenant_id, campaign_id, title, description, promo_code)
    SELECT c.tenant_id, c.id,
           'Demo coffee and snack combo',
           'Fictional offer for local UI testing.',
           'DEMOCOFFEE'
    FROM new_campaign c
    WHERE NOT EXISTS (
        SELECT 1
        FROM campaign_offers
        WHERE campaign_id = c.id
          AND title = 'Demo coffee and snack combo'
    )
),
new_channel AS (
    INSERT INTO campaign_channels (tenant_id, campaign_id, channel, allocated_budget)
    SELECT c.tenant_id, c.id, 'GOOGLE_ADS', c.total_budget / 2
    FROM new_campaign c
    WHERE NOT EXISTS (
        SELECT 1
        FROM campaign_channels
        WHERE campaign_id = c.id
          AND channel = 'GOOGLE_ADS'
    )
),
new_audience AS (
    INSERT INTO audiences (tenant_id, campaign_id, name, definition)
    SELECT c.tenant_id, c.id,
           'Demo local coffee enthusiasts',
           '{"ageMin":21,"ageMax":55,"interests":["coffee","cafes"]}'::jsonb
    FROM new_campaign c
    WHERE NOT EXISTS (
        SELECT 1
        FROM audiences
        WHERE campaign_id = c.id
          AND name = 'Demo local coffee enthusiasts'
    )
),
new_geo AS (
    INSERT INTO geo_targets (
        tenant_id, campaign_id, target_type, name, latitude, longitude,
        radius_km, country_code, city
    )
    SELECT c.tenant_id, c.id,
           'RADIUS', 'Demo Sector 18 catchment', 28.5700, 77.3200, 3, 'IN', 'Noida'
    FROM new_campaign c
    WHERE NOT EXISTS (
        SELECT 1
        FROM geo_targets
        WHERE campaign_id = c.id
          AND name = 'Demo Sector 18 catchment'
    )
),
new_creative AS (
    INSERT INTO creatives (
        tenant_id, campaign_id, channel, headline, body, call_to_action, status
    )
    SELECT c.tenant_id, c.id, 'GOOGLE_ADS',
           'Your neighbourhood coffee break',
           'Fictional creative for UI and workflow testing.',
           'Learn more', 'DRAFT'
    FROM new_campaign c
    WHERE NOT EXISTS (
        SELECT 1
        FROM creatives
        WHERE campaign_id = c.id
          AND channel = 'GOOGLE_ADS'
    )
),
new_lead AS (
    INSERT INTO leads (
        tenant_id, business_id, campaign_id, name, email, phone, status, source
    )
    SELECT c.tenant_id, c.business_id, c.id,
           'Demo Customer', 'demo.customer@example.com', '+91 98765 0001',
           'QUALIFIED', 'NEON_DEMO'
    FROM new_campaign c
    WHERE NOT EXISTS (
        SELECT 1
        FROM leads
        WHERE campaign_id = c.id
          AND email = 'demo.customer@example.com'
    )
),
new_metric AS (
    INSERT INTO campaign_metrics (
        tenant_id, campaign_id, metric_date, spend, impressions, reach,
        clicks, conversions, leads
    )
    SELECT c.tenant_id, c.id,
           current_date - 1, 320.00, 2500, 1800, 115, 7, 4
    FROM new_campaign c
    WHERE NOT EXISTS (
        SELECT 1
        FROM campaign_metrics
        WHERE campaign_id = c.id
          AND metric_date = current_date - 1
    )
),
new_platform AS (
    INSERT INTO platform_connections (
        tenant_id, platform, external_account_id, status
    )
    SELECT t.tenant_id, 'GOOGLE', 'demo-google-account', 'CONNECTED'
    FROM tenant_seed t
    WHERE NOT EXISTS (
        SELECT 1
        FROM platform_connections
        WHERE tenant_id = t.tenant_id
          AND platform = 'GOOGLE'
    )
),
new_recommendation AS (
    INSERT INTO ai_recommendations (
        tenant_id, campaign_id, recommendation_type, prompt_version,
        provider, model, payload, status
    )
    SELECT c.tenant_id, c.id,
           'BUDGET_OPTIMIZATION', 'v1', 'MOCK', 'mock-model',
           '{"suggestion":"Increase the mobile audience budget"}'::jsonb,
           'GENERATED'
    FROM new_campaign c
    WHERE NOT EXISTS (
        SELECT 1
        FROM ai_recommendations
        WHERE campaign_id = c.id
          AND recommendation_type = 'BUDGET_OPTIMIZATION'
    )
),
new_ai_usage AS (
    INSERT INTO ai_usage_log (
        tenant_id, user_id, recommendation_type, provider, model,
        prompt_version, input_tokens, output_tokens, estimated_cost_usd,
        duration_ms, status
    )
    SELECT t.tenant_id, u.id, 'BUDGET_OPTIMIZATION', 'MOCK', 'mock-model',
           'v1', 120, 45, 0.00, 350, 'SUCCESS'
    FROM tenant_seed t
    CROSS JOIN users u
    WHERE u.email = 'owner@example.com'
      AND NOT EXISTS (
          SELECT 1
          FROM ai_usage_log
          WHERE tenant_id = t.tenant_id
            AND user_id = u.id
            AND recommendation_type = 'BUDGET_OPTIMIZATION'
      )
),
new_audit AS (
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
    'tenant', 'Fixna Demo Workspace',
    'user', 'owner@example.com',
    'business', 'Demo Neighbourhood Cafe',
    'campaign', 'DEMO - Cafe opening results',
    'result', 'demo data ready'
) AS result;
