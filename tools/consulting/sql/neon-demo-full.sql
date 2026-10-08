-- Fixna Consulting full demo dataset for Neon.
-- Run after the Consulting Flyway migrations are applied.
-- This script is idempotent and safe to run repeatedly in the Neon SQL Editor.

BEGIN;

-- Demo consultant password: replace the hash below with a BCrypt hash for a login you control.
-- The current placeholder is intentionally not a valid password and must be replaced.
WITH password_seed AS (
    SELECT '$2b$12$REPLACE_WITH_YOUR_BCRYPT_HASH'::VARCHAR(255) AS password_hash
),
new_staff_user AS (
    INSERT INTO users (email, password_hash, first_name, last_name)
    SELECT 'consultant@example.com', password_hash, 'Demo', 'Consultant'
    FROM password_seed
    WHERE NOT EXISTS (
        SELECT 1 FROM users WHERE email = 'consultant@example.com'
    )
    RETURNING id
),
new_tenant AS (
    INSERT INTO tenants (name, tenant_type)
    SELECT 'Fixna Consulting Demo', 'CONSULTING_WORKSPACE'
    WHERE NOT EXISTS (
        SELECT 1 FROM tenants WHERE name = 'Fixna Consulting Demo'
    )
    RETURNING id
),
tenant_seed AS (
    SELECT COALESCE(
        (SELECT id FROM tenants WHERE name = 'Fixna Consulting Demo'),
        (SELECT id FROM new_tenant)
    ) AS tenant_id
),
new_staff_membership AS (
    INSERT INTO tenant_memberships (tenant_id, user_id, role)
    SELECT t.tenant_id, u.id, 'CONSULTANT_ADMIN'
    FROM tenant_seed t
    CROSS JOIN users u
    WHERE u.email = 'consultant@example.com'
      AND NOT EXISTS (
          SELECT 1
          FROM tenant_memberships tm
          WHERE tm.tenant_id = t.tenant_id
            AND tm.user_id = u.id
      )
),
new_profile AS (
    INSERT INTO consulting_profiles (
        tenant_id, display_name, tagline, bio, public_slug, is_published
    )
    SELECT t.tenant_id,
           'Fixna Consulting Demo',
           'Strategy for growing firms',
           'Synthetic workspace covering the full consulting workflow.',
           'demo-consulting',
           TRUE
    FROM tenant_seed t
    WHERE NOT EXISTS (
        SELECT 1
        FROM consulting_profiles
        WHERE tenant_id = t.tenant_id
    )
),
new_client AS (
    INSERT INTO clients (
        tenant_id, name, email, industry, status
    )
    SELECT t.tenant_id, 'Acme Retail Group', 'contact@acme-demo.example',
           'Retail', 'ACTIVE'
    FROM tenant_seed t
    WHERE NOT EXISTS (
        SELECT 1 FROM clients
        WHERE tenant_id = t.tenant_id
          AND email = 'contact@acme-demo.example'
    )
    RETURNING id, tenant_id
),
new_contact AS (
    INSERT INTO client_contacts (
        tenant_id, client_id, name, email, job_title, is_primary
    )
    SELECT c.tenant_id, c.id, 'Jane Acme', 'jane@acme-demo.example',
           'COO', TRUE
    FROM new_client c
    WHERE NOT EXISTS (
        SELECT 1
        FROM client_contacts
        WHERE client_id = c.id
          AND email = 'jane@acme-demo.example'
    )
),
new_service AS (
    INSERT INTO consulting_services (
        tenant_id, name, description, price_amount, price_currency,
        duration_minutes, active, sort_order
    )
    SELECT t.tenant_id,
           'Strategy workshop',
           'Half-day executive strategy session.',
           2500.00,
           'USD',
           240,
           TRUE,
           1
    FROM tenant_seed t
    WHERE NOT EXISTS (
        SELECT 1
        FROM consulting_services
        WHERE tenant_id = t.tenant_id
          AND name = 'Strategy workshop'
    )
),
new_engagement AS (
    INSERT INTO engagements (
        tenant_id, client_id, title, description, status, start_date, end_date
    )
    SELECT c.tenant_id, c.id,
           'Q4 Growth Planning',
           'Demo engagement for local testing.',
           'ACTIVE',
           current_date,
           current_date + 90
    FROM new_client c
    WHERE NOT EXISTS (
        SELECT 1
        FROM engagements
        WHERE tenant_id = c.tenant_id
          AND title = 'Q4 Growth Planning'
    )
    RETURNING id, tenant_id, client_id
),
new_project AS (
    INSERT INTO projects (
        tenant_id, engagement_id, name, description, status, start_date, end_date
    )
    SELECT e.tenant_id, e.id,
           'Discovery & roadmap',
           'Initial project phase.',
           'IN_PROGRESS',
           current_date,
           current_date + 30
    FROM new_engagement e
    WHERE NOT EXISTS (
        SELECT 1
        FROM projects
        WHERE engagement_id = e.id
          AND name = 'Discovery & roadmap'
    )
    RETURNING id, tenant_id, engagement_id
),
new_milestone AS (
    INSERT INTO milestones (
        tenant_id, project_id, name, description, status, due_date, sort_order
    )
    SELECT p.tenant_id, p.id,
           'Stakeholder interviews',
           'Identify priorities and issues.',
           'IN_PROGRESS',
           current_date + 7,
           1
    FROM new_project p
    WHERE NOT EXISTS (
        SELECT 1
        FROM milestones
        WHERE project_id = p.id
          AND name = 'Stakeholder interviews'
    )
    RETURNING id, project_id
),
new_task AS (
    INSERT INTO tasks (
        tenant_id, project_id, milestone_id, title, description, status, due_date
    )
    SELECT p.tenant_id, p.id, m.id,
           'Schedule executive interviews',
           'Coordinate stakeholder meetings.',
           'TODO',
           current_date + 3
    FROM new_project p
    JOIN new_milestone m ON m.project_id = p.id
    WHERE NOT EXISTS (
        SELECT 1
        FROM tasks
        WHERE project_id = p.id
          AND title = 'Schedule executive interviews'
    )
),
new_meeting AS (
    INSERT INTO meetings (
        tenant_id, client_id, engagement_id, project_id, title,
        description, starts_at, ends_at, organizer_user_id, status,
        calendar_provider
    )
    SELECT e.tenant_id, e.client_id, e.id, p.id,
           'Kickoff workshop',
           'Demo kickoff for the growth planning engagement.',
           now() + interval '2 days',
           now() + interval '2 days 2 hours',
           u.id,
           'SCHEDULED',
           'INTERNAL'
    FROM new_engagement e
    JOIN new_project p ON p.engagement_id = e.id
    CROSS JOIN users u
    WHERE u.email = 'consultant@example.com'
      AND NOT EXISTS (
          SELECT 1
          FROM meetings
          WHERE client_id = e.client_id
            AND title = 'Kickoff workshop'
      )
),
new_proposal AS (
    INSERT INTO proposals (
        tenant_id, client_id, engagement_id, title, description,
        status, subtotal, tax_amount, total_amount, currency, valid_until
    )
    SELECT e.tenant_id, e.client_id, e.id,
           'Strategy engagement proposal',
           'Proposal for the discovery and roadmap engagement.',
           'DRAFT', 5000.00, 500.00, 5500.00, 'USD', current_date + 30
    FROM new_engagement e
    WHERE NOT EXISTS (
        SELECT 1
        FROM proposals
        WHERE engagement_id = e.id
          AND title = 'Strategy engagement proposal'
    )
    RETURNING id, tenant_id, client_id, engagement_id
),
new_proposal_item AS (
    INSERT INTO proposal_items (
        tenant_id, proposal_id, description, quantity, unit_price, line_total,
        sort_order
    )
    SELECT p.tenant_id, p.id,
           'Discovery workshops (2 days)', 2, 2500.00, 5000.00, 1
    FROM new_proposal p
    WHERE NOT EXISTS (
        SELECT 1
        FROM proposal_items
        WHERE proposal_id = p.id
    )
),
new_invoice AS (
    INSERT INTO invoices (
        tenant_id, client_id, engagement_id, proposal_id, invoice_number,
        title, status, subtotal, tax_amount, total_amount, amount_paid,
        currency, due_date
    )
    SELECT p.tenant_id, p.client_id, p.engagement_id, p.id,
           'INV-DEMO-001',
           'Strategy phase 1',
           'DRAFT', 5000.00, 500.00, 5500.00, 0.00, 'USD', current_date + 30
    FROM new_proposal p
    WHERE NOT EXISTS (
        SELECT 1
        FROM invoices
        WHERE tenant_id = p.tenant_id
          AND invoice_number = 'INV-DEMO-001'
    )
    RETURNING id, tenant_id, client_id
),
new_invoice_line AS (
    INSERT INTO invoice_lines (
        tenant_id, invoice_id, description, quantity, unit_price, line_total,
        sort_order
    )
    SELECT i.tenant_id, i.id,
           'Discovery workshops (2 days)', 2, 2500.00, 5000.00, 1
    FROM new_invoice i
    WHERE NOT EXISTS (
        SELECT 1
        FROM invoice_lines
        WHERE invoice_id = i.id
    )
),
new_payment AS (
    INSERT INTO payments (
        tenant_id, invoice_id, amount, currency, payment_method, reference,
        recorded_by_user_id
    )
    SELECT i.tenant_id, i.id,
           1000.00, 'USD', 'CARD', 'DEMO-PAYMENT-001', u.id
    FROM new_invoice i
    CROSS JOIN users u
    WHERE u.email = 'consultant@example.com'
      AND NOT EXISTS (
          SELECT 1
          FROM payments
          WHERE invoice_id = i.id
            AND reference = 'DEMO-PAYMENT-001'
      )
),
new_testimonial AS (
    INSERT INTO testimonials (
        tenant_id, author_name, author_title, quote, published, sort_order
    )
    SELECT t.tenant_id,
           'Alex Demo',
           'CEO, Acme Retail',
           'Synthetic testimonial for local UI testing only.',
           TRUE,
           1
    FROM tenant_seed t
    WHERE NOT EXISTS (
        SELECT 1
        FROM testimonials
        WHERE tenant_id = t.tenant_id
          AND author_name = 'Alex Demo'
    )
),
new_portal_user AS (
    INSERT INTO users (email, password_hash, first_name, last_name)
    SELECT 'client@example.com', password_hash, 'Portal', 'User'
    FROM password_seed
    WHERE NOT EXISTS (
        SELECT 1 FROM users WHERE email = 'client@example.com'
    )
    RETURNING id
),
new_portal_membership AS (
    INSERT INTO client_portal_memberships (tenant_id, client_id, user_id, role)
    SELECT c.tenant_id, c.id, u.id, 'CLIENT_USER'
    FROM new_client c
    CROSS JOIN new_portal_user u
    WHERE NOT EXISTS (
        SELECT 1
        FROM client_portal_memberships
        WHERE client_id = c.id
          AND user_id = u.id
    )
),
new_ai_usage AS (
    INSERT INTO ai_usage_logs (tenant_id, user_id, feature, request_summary)
    SELECT t.tenant_id, u.id, 'AI_ASSISTANT',
           'Demo request: summarize the Q4 Growth Planning engagement.'
    FROM tenant_seed t
    CROSS JOIN users u
    WHERE u.email = 'consultant@example.com'
      AND NOT EXISTS (
          SELECT 1
          FROM ai_usage_logs
          WHERE tenant_id = t.tenant_id
            AND user_id = u.id
            AND feature = 'AI_ASSISTANT'
      )
)
SELECT 1;

COMMIT;

SELECT json_build_object(
    'tenant', 'Fixna Consulting Demo',
    'staff_user', 'consultant@example.com',
    'client_user', 'client@example.com',
    'client', 'Acme Retail Group',
    'engagement', 'Q4 Growth Planning',
    'result', 'demo data ready'
) AS result;
