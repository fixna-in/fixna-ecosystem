
-- ============================================================
-- LOCAL DEVELOPMENT ONLY
-- pgAdmin / PostgreSQL SQL version
--
-- Target:
--   Database : consulting
--   Host     : localhost
--   Port     : 5433
--
-- IMPORTANT:
--   Replace the value of seed_password_hash below with a valid
--   BCrypt hash before executing this script.
--
-- Do NOT put a plaintext password here.
--
-- Requires Flyway migrations already applied.
-- ============================================================

BEGIN;

-- ============================================================
-- 1. LOCAL DATABASE SAFETY CHECK
-- ============================================================

DO $$
BEGIN
    IF current_database() <> 'consulting' THEN
        RAISE EXCEPTION
            'Refusing seed: connected to database "%". Expected "consulting".',
            current_database();
    END IF;

    IF current_setting('port') <> '5432' THEN
        RAISE EXCEPTION
            'Refusing seed: connected to PostgreSQL port %. Expected 5433.',
            current_setting('port');
    END IF;

    IF inet_server_addr() IS NOT NULL
       AND inet_server_addr() NOT IN (
           '127.0.0.1'::inet,
           '::1'::inet
       ) THEN
        RAISE EXCEPTION
            'Refusing seed: PostgreSQL server is not localhost. Server address: %',
            inet_server_addr();
    END IF;
END
$$;


-- ============================================================
-- 2. TEST PASSWORD HASH
-- ============================================================
--
-- Replace ONLY the value below.
--
-- Example format:
-- $2b$12$.....................................................
--
-- Do NOT use a plaintext password.
-- ============================================================

DO $$
DECLARE
    seed_password_hash TEXT := '$2a$10$BAFhp4UGC7YVYh9WjPHndeYV7CnoADpM2Q2IpS6.CJ/xnjsfBE1K2';
BEGIN

    IF seed_password_hash = '$2a$10$BAFhp4UGC7YVYh9WjPHndeYV7CnoADpM2Q2IpS6.CJ/xnjsfBE1K2'
        RAISE EXCEPTION
            'Replace REPLACE_WITH_YOUR_BCRYPT_HASH with a real BCrypt hash before running the seed.';
    END IF;

    IF seed_password_hash !~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$' THEN
        RAISE EXCEPTION
            'Invalid BCrypt hash supplied for demo user.';
    END IF;

END
$$;


-- ============================================================
-- 3. DEMO DATA
-- ============================================================
--
-- Local test fixture only.
--
-- If consultant@example.com already exists:
--   new_user inserts zero rows
--   therefore the complete demo workspace is skipped.
-- ============================================================

WITH seed_config AS (
    SELECT '$2b$12$REPLACE_WITH_YOUR_BCRYPT_HASH'::VARCHAR(255)
        AS password_hash
),

new_user AS (
    INSERT INTO users (
        email,
        password_hash,
        first_name,
        last_name
    )
    SELECT
        'consultant@example.com',
        password_hash,
        'Demo',
        'Consultant'
    FROM seed_config
    ON CONFLICT (email) DO NOTHING
    RETURNING id
),

new_tenant AS (
    INSERT INTO tenants (
        name,
        tenant_type
    )
    SELECT
        'Fixna Consulting Demo',
        'CONSULTING_WORKSPACE'
    FROM new_user
    RETURNING id
),

new_membership AS (
    INSERT INTO tenant_memberships (
        tenant_id,
        user_id,
        role
    )
    SELECT
        t.id,
        u.id,
        'CONSULTANT_ADMIN'
    FROM new_tenant t
    CROSS JOIN new_user u
    RETURNING tenant_id, user_id
),

new_profile AS (
    INSERT INTO consulting_profiles (
        tenant_id,
        display_name,
        tagline,
        bio,
        public_slug,
        is_published
    )
    SELECT
        tenant_id,
        'Fixna Consulting Demo',
        'Strategy for growing firms',
        'Synthetic local demo workspace — not a real consultancy.',
        'demo-consulting',
        TRUE
    FROM new_membership
    RETURNING tenant_id
),

new_client AS (
    INSERT INTO clients (
        tenant_id,
        name,
        email,
        industry,
        status
    )
    SELECT
        tenant_id,
        'Acme Retail Group',
        'contact@acme-demo.example',
        'Retail',
        'ACTIVE'
    FROM new_membership
    RETURNING id, tenant_id
),

new_contact AS (
    INSERT INTO client_contacts (
        tenant_id,
        client_id,
        name,
        email,
        job_title,
        is_primary
    )
    SELECT
        c.tenant_id,
        c.id,
        'Jane Acme',
        'jane@acme-demo.example',
        'COO',
        TRUE
    FROM new_client c
    RETURNING client_id, tenant_id
),

new_service AS (
    INSERT INTO consulting_services (
        tenant_id,
        name,
        description,
        price_amount,
        price_currency,
        duration_minutes,
        active,
        sort_order
    )
    SELECT
        tenant_id,
        'Strategy workshop',
        'Half-day executive strategy session',
        2500.00,
        'USD',
        240,
        TRUE,
        1
    FROM new_membership
    RETURNING id, tenant_id
),

new_engagement AS (
    INSERT INTO engagements (
        tenant_id,
        client_id,
        title,
        description,
        status,
        start_date,
        end_date
    )
    SELECT
        c.tenant_id,
        c.id,
        'Q4 Growth Planning',
        'Demo engagement for local testing',
        'ACTIVE',
        current_date,
        current_date + 90
    FROM new_client c
    RETURNING id, tenant_id, client_id
),

new_project AS (
    INSERT INTO projects (
        tenant_id,
        engagement_id,
        name,
        description,
        status,
        start_date,
        end_date
    )
    SELECT
        e.tenant_id,
        e.id,
        'Discovery & roadmap',
        'Initial project phase',
        'IN_PROGRESS',
        current_date,
        current_date + 30
    FROM new_engagement e
    RETURNING id, tenant_id, engagement_id
),

new_milestone AS (
    INSERT INTO milestones (
        tenant_id,
        project_id,
        name,
        status,
        due_date,
        sort_order
    )
    SELECT
        p.tenant_id,
        p.id,
        'Stakeholder interviews',
        'IN_PROGRESS',
        current_date + 7,
        1
    FROM new_project p
    RETURNING id, tenant_id, project_id
),

new_task AS (
    INSERT INTO tasks (
        tenant_id,
        project_id,
        milestone_id,
        title,
        status,
        due_date
    )
    SELECT
        p.tenant_id,
        p.id,
        m.id,
        'Schedule executive interviews',
        'TODO',
        current_date + 3
    FROM new_project p
    JOIN new_milestone m
        ON m.project_id = p.id
    RETURNING id, tenant_id
),

new_meeting AS (
    INSERT INTO meetings (
        tenant_id,
        client_id,
        engagement_id,
        project_id,
        title,
        starts_at,
        ends_at,
        organizer_user_id,
        status,
        calendar_provider
    )
    SELECT
        e.tenant_id,
        e.client_id,
        e.id,
        p.id,
        'Kickoff workshop',
        now() + INTERVAL '2 days',
        now() + INTERVAL '2 days 2 hours',
        m.user_id,
        'SCHEDULED',
        'INTERNAL'
    FROM new_engagement e
    JOIN new_project p
        ON p.engagement_id = e.id
    CROSS JOIN new_membership m
    RETURNING id, tenant_id
),

new_proposal AS (
    INSERT INTO proposals (
        tenant_id,
        client_id,
        engagement_id,
        title,
        status,
        subtotal,
        tax_amount,
        total_amount,
        currency
    )
    SELECT
        e.tenant_id,
        e.client_id,
        e.id,
        'Strategy engagement proposal',
        'DRAFT',
        5000.00,
        500.00,
        5500.00,
        'USD'
    FROM new_engagement e
    RETURNING id, tenant_id, engagement_id, client_id
),

new_proposal_item AS (
    INSERT INTO proposal_items (
        tenant_id,
        proposal_id,
        description,
        quantity,
        unit_price,
        line_total,
        sort_order
    )
    SELECT
        tenant_id,
        id,
        'Discovery workshops (2 days)',
        2,
        2500.00,
        5000.00,
        1
    FROM new_proposal
    RETURNING proposal_id, tenant_id
),

new_invoice AS (
    INSERT INTO invoices (
        tenant_id,
        client_id,
        engagement_id,
        proposal_id,
        invoice_number,
        title,
        status,
        subtotal,
        tax_amount,
        total_amount,
        currency,
        due_date
    )
    SELECT
        p.tenant_id,
        p.client_id,
        p.engagement_id,
        p.id,
        'INV-DEMO-001',
        'Strategy phase 1',
        'DRAFT',
        5000.00,
        500.00,
        5500.00,
        'USD',
        current_date + 30
    FROM new_proposal p
    RETURNING id, tenant_id
),

new_invoice_line AS (
    INSERT INTO invoice_lines (
        tenant_id,
        invoice_id,
        description,
        quantity,
        unit_price,
        line_total,
        sort_order
    )
    SELECT
        tenant_id,
        id,
        'Discovery workshops (2 days)',
        2,
        2500.00,
        5000.00,
        1
    FROM new_invoice
    RETURNING invoice_id, tenant_id
),

new_testimonial AS (
    INSERT INTO testimonials (
        tenant_id,
        author_name,
        author_title,
        quote,
        published,
        sort_order
    )
    SELECT
        tenant_id,
        'Alex Demo',
        'CEO, Acme Retail',
        'Synthetic testimonial for local UI testing only.',
        TRUE,
        1
    FROM new_membership
    RETURNING id, tenant_id
),

portal_user AS (
    INSERT INTO users (
        email,
        password_hash,
        first_name,
        last_name
    )
    SELECT
        'client@example.com',
        password_hash,
        'Portal',
        'User'
    FROM seed_config
    ON CONFLICT (email) DO NOTHING
    RETURNING id
),

portal_membership AS (
    INSERT INTO client_portal_memberships (
        tenant_id,
        client_id,
        user_id,
        role
    )
    SELECT
        c.tenant_id,
        c.id,
        u.id,
        'CLIENT_USER'
    FROM new_client c
    CROSS JOIN portal_user u
    RETURNING id
)

SELECT
    count(*) AS testimonials_created
FROM new_testimonial;


-- ============================================================
-- 4. COMMIT
-- ============================================================

COMMIT;

