-- Fixna Invoice Assistant full demo dataset for Neon.
-- Run after the Invoice Assistant Flyway migrations are applied.
-- This script is idempotent and safe to run repeatedly in the Neon SQL Editor.

BEGIN;

-- Demo password: replace the hash below with a BCrypt hash for a login you control.
-- The current placeholder is intentionally not a valid password and must be replaced.
WITH password_seed AS (
    SELECT '*******************'::VARCHAR(255) AS password_hash
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
    SELECT 'Fixna Invoice Assistant Demo', 'SMB'
    WHERE NOT EXISTS (
        SELECT 1 FROM tenants WHERE name = 'Fixna Invoice Assistant Demo'
    )
    RETURNING id
),
tenant_seed AS (
    SELECT COALESCE(
        (SELECT id FROM tenants WHERE name = 'Fixna Invoice Assistant Demo'),
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
new_invoice_record AS (
    INSERT INTO invoice_records (
        tenant_id, vendor_name, invoice_number, invoice_date, due_date,
        currency, subtotal, tax_total, grand_total, status, created_by_user_id
    )
    SELECT t.tenant_id,
           'Acme Supplies Ltd',
           'INV-2024-001',
           current_date - interval '15 days',
           current_date + interval '15 days',
           'INR',
           50000.00,
           9000.00,
           59000.00,
           'APPROVED',
           u.id
    FROM tenant_seed t
    CROSS JOIN users u
    WHERE u.email = 'owner@example.com'
      AND NOT EXISTS (
          SELECT 1
          FROM invoice_records
          WHERE tenant_id = t.tenant_id
            AND invoice_number = 'INV-2024-001'
      )
    RETURNING id, tenant_id
),
new_invoice_lines AS (
    INSERT INTO invoice_record_lines (tenant_id, invoice_record_id, description, quantity, unit_price, line_total, sort_order)
    SELECT i.tenant_id, i.id, 'Office supplies - paper, pens, folders', 1, 15000.00, 15000.00, 1
    FROM new_invoice_record i
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_record_lines WHERE invoice_record_id = i.id AND sort_order = 1
    )
),
new_invoice_lines2 AS (
    INSERT INTO invoice_record_lines (tenant_id, invoice_record_id, description, quantity, unit_price, line_total, sort_order)
    SELECT i.tenant_id, i.id, 'Printer ink cartridges (set of 4)', 2, 12000.00, 24000.00, 2
    FROM new_invoice_record i
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_record_lines WHERE invoice_record_id = i.id AND sort_order = 2
    )
),
new_invoice_lines3 AS (
    INSERT INTO invoice_record_lines (tenant_id, invoice_record_id, description, quantity, unit_price, line_total, sort_order)
    SELECT i.tenant_id, i.id, 'Cloud storage subscription (annual)', 1, 11000.00, 11000.00, 3
    FROM new_invoice_record i
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_record_lines WHERE invoice_record_id = i.id AND sort_order = 3
    )
),
new_document AS (
    INSERT INTO invoice_documents (
        tenant_id, invoice_record_id, file_name, storage_key, content_type,
        size_bytes, sha256, status, uploaded_by_user_id, purge_after
    )
    SELECT i.tenant_id, i.id,
           'invoice-INV-2024-001.pdf',
           'uploads/demo/invoice-INV-2024-001.pdf',
           'application/pdf',
           245000,
           encode(digest('demo-invoice-content', 'sha256'), 'hex'),
           'APPROVED',
           u.id,
           now() + interval '2 years'
    FROM new_invoice_record i
    CROSS JOIN users u
    WHERE u.email = 'owner@example.com'
      AND NOT EXISTS (
          SELECT 1
          FROM invoice_documents
          WHERE tenant_id = i.tenant_id
            AND invoice_record_id = i.id
            AND file_name = 'invoice-INV-2024-001.pdf'
      )
    RETURNING id, tenant_id, invoice_record_id
),
new_extraction AS (
    INSERT INTO invoice_document_extractions (
        tenant_id, document_id, status, method_native_pages, method_ocr_pages,
        extractor_version, approved_by_user_id, approved_at, posted_invoice_record_id
    )
    SELECT d.tenant_id, d.id, 'POSTED', 2, 0, 'v1.2.0', u.id, now(), d.invoice_record_id
    FROM new_document d
    CROSS JOIN users u
    WHERE u.email = 'owner@example.com'
      AND NOT EXISTS (
          SELECT 1
          FROM invoice_document_extractions
          WHERE document_id = d.id
      )
    RETURNING id, tenant_id
),
new_fields AS (
    INSERT INTO invoice_document_fields (tenant_id, extraction_id, field_key, field_value, money_amount, currency, confidence, source, page, extractor_version)
    SELECT e.tenant_id, e.id, 'vendor_name', 'Acme Supplies Ltd', NULL, NULL, 0.98, 'NATIVE_TEXT', 1, 'v1.2.0'
    FROM new_extraction e
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_document_fields WHERE extraction_id = e.id AND field_key = 'vendor_name'
    )
),
new_fields2 AS (
    INSERT INTO invoice_document_fields (tenant_id, extraction_id, field_key, field_value, money_amount, currency, confidence, source, page, extractor_version)
    SELECT e.tenant_id, e.id, 'invoice_number', 'INV-2024-001', NULL, NULL, 0.99, 'NATIVE_TEXT', 1, 'v1.2.0'
    FROM new_extraction e
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_document_fields WHERE extraction_id = e.id AND field_key = 'invoice_number'
    )
),
new_fields3 AS (
    INSERT INTO invoice_document_fields (tenant_id, extraction_id, field_key, field_value, money_amount, currency, confidence, source, page, extractor_version)
    SELECT e.tenant_id, e.id, 'invoice_date', '2024-10-01', NULL, NULL, 0.97, 'NATIVE_TEXT', 1, 'v1.2.0'
    FROM new_extraction e
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_document_fields WHERE extraction_id = e.id AND field_key = 'invoice_date'
    )
),
new_fields4 AS (
    INSERT INTO invoice_document_fields (tenant_id, extraction_id, field_key, field_value, money_amount, currency, confidence, source, page, extractor_version)
    SELECT e.tenant_id, e.id, 'due_date', '2024-10-31', NULL, NULL, 0.97, 'NATIVE_TEXT', 1, 'v1.2.0'
    FROM new_extraction e
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_document_fields WHERE extraction_id = e.id AND field_key = 'due_date'
    )
),
new_fields5 AS (
    INSERT INTO invoice_document_fields (tenant_id, extraction_id, field_key, field_value, money_amount, currency, confidence, source, page, extractor_version)
    SELECT e.tenant_id, e.id, 'grand_total', NULL, 59000.00, 'INR', 0.99, 'NATIVE_TEXT', 1, 'v1.2.0'
    FROM new_extraction e
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_document_fields WHERE extraction_id = e.id AND field_key = 'grand_total'
    )
),
new_fields6 AS (
    INSERT INTO invoice_document_fields (tenant_id, extraction_id, field_key, field_value, money_amount, currency, confidence, source, page, extractor_version)
    SELECT e.tenant_id, e.id, 'tax_total', NULL, 9000.00, 'INR', 0.98, 'NATIVE_TEXT', 1, 'v1.2.0'
    FROM new_extraction e
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_document_fields WHERE extraction_id = e.id AND field_key = 'tax_total'
    )
),
new_job AS (
    INSERT INTO invoice_document_jobs (tenant_id, document_id, status, started_at, completed_at)
    SELECT d.tenant_id, d.id, 'COMPLETED', now() - interval '1 hour', now() - interval '50 minutes'
    FROM new_document d
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_document_jobs WHERE document_id = d.id
    )
),
new_quota AS (
    INSERT INTO tenant_quotas (tenant_id, uploads_this_month, extractions_this_month, reset_at)
    SELECT t.tenant_id, 1, 1, now() + interval '1 month'
    FROM tenant_seed t
    ON CONFLICT (tenant_id) DO UPDATE SET
        uploads_this_month = tenant_quotas.uploads_this_month + 1,
        extractions_this_month = tenant_quotas.extractions_this_month + 1
)
SELECT 1;

COMMIT;

SELECT json_build_object(
    'tenant', 'Fixna Invoice Assistant Demo',
    'user', 'owner@example.com',
    'invoice_record', 'INV-2024-001',
    'result', 'demo data ready'
) AS result;