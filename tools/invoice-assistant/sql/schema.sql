-- LOCAL ONLY. Run with psql -X -h localhost -U postgres -d invoice_assistant -f schema.sql
-- Snapshot of immutable Flyway migrations. Empty public schema required.
-- Do not run on an existing migrated database. Future changes use Flyway.
BEGIN;
DO $$ BEGIN
  IF inet_server_addr() IS NOT NULL AND inet_server_addr() NOT IN ('127.0.0.1'::inet, '::1'::inet) THEN
    RAISE EXCEPTION 'Local PostgreSQL only';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public') THEN
    RAISE EXCEPTION 'Public schema is not empty; use Flyway for existing databases';
  END IF;
END $$;
SET LOCAL search_path = public;

-- Source: V1__platform_core.sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    tenant_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tenant_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_membership_user ON tenant_memberships(user_id);
CREATE INDEX idx_membership_tenant ON tenant_memberships(tenant_id);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(100),
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_tenant_time ON audit_logs(tenant_id, created_at);

-- Source: V2__invoice_records.sql
CREATE TABLE invoice_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    vendor_name TEXT,
    invoice_number VARCHAR(100),
    invoice_date DATE,
    due_date DATE,
    currency CHAR(3),
    subtotal NUMERIC(14,2),
    tax_total NUMERIC(14,2),
    grand_total NUMERIC(14,2),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'NEEDS_REVIEW', 'APPROVED', 'POSTED', 'VOID')),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoice_records_tenant ON invoice_records(tenant_id, created_at DESC);

CREATE TABLE invoice_record_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    invoice_record_id UUID NOT NULL REFERENCES invoice_records(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    quantity NUMERIC(14,3) NOT NULL DEFAULT 1,
    unit_price NUMERIC(14,2),
    line_total NUMERIC(14,2),
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_invoice_record_lines_invoice ON invoice_record_lines(invoice_record_id);

-- Source: V3__invoice_documents.sql
CREATE TABLE invoice_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    invoice_record_id UUID REFERENCES invoice_records(id) ON DELETE SET NULL,
    file_name TEXT NOT NULL,
    storage_key TEXT,
    content_type VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    sha256 VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED'
        CHECK (status IN ('RECEIVED', 'NEEDS_REVIEW', 'APPROVED', 'POSTED', 'REJECTED', 'DELETED')),
    uploaded_by_user_id UUID NOT NULL REFERENCES users(id),
    purge_after TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoice_documents_tenant_record ON invoice_documents(tenant_id, invoice_record_id);
CREATE UNIQUE INDEX uq_invoice_documents_dedupe ON invoice_documents(tenant_id, invoice_record_id, sha256);
CREATE INDEX idx_invoice_documents_purge
    ON invoice_documents(tenant_id, purge_after) WHERE status <> 'DELETED';

CREATE TABLE invoice_document_extractions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    document_id UUID NOT NULL REFERENCES invoice_documents(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'NEEDS_REVIEW'
        CHECK (status IN ('NEEDS_REVIEW', 'APPROVED', 'REJECTED', 'POSTED')),
    method_native_pages INT NOT NULL DEFAULT 0,
    method_ocr_pages INT NOT NULL DEFAULT 0,
    extractor_version VARCHAR(40) NOT NULL,
    approved_by_user_id UUID REFERENCES users(id),
    approved_at TIMESTAMPTZ,
    posted_invoice_record_id UUID REFERENCES invoice_records(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoice_extractions_document ON invoice_document_extractions(document_id);
CREATE INDEX idx_invoice_extractions_tenant ON invoice_document_extractions(tenant_id);

CREATE TABLE invoice_document_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    extraction_id UUID NOT NULL REFERENCES invoice_document_extractions(id) ON DELETE CASCADE,
    field_key VARCHAR(60) NOT NULL,
    field_value TEXT,
    money_amount NUMERIC(14,2),
    currency CHAR(3),
    confidence NUMERIC(3,2) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    source VARCHAR(20) NOT NULL
        CHECK (source IN ('NATIVE_TEXT', 'OCR', 'AI_ADVISORY', 'MANUAL')),
    page INT,
    bbox JSONB,
    raw_snippet TEXT,
    extractor_version VARCHAR(40) NOT NULL,
    confirmed_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoice_fields_extraction ON invoice_document_fields(extraction_id, field_key);
CREATE INDEX idx_invoice_fields_tenant ON invoice_document_fields(tenant_id);

-- Source: V4__invoice_document_jobs.sql
CREATE TABLE invoice_document_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    document_id UUID NOT NULL REFERENCES invoice_documents(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    error_code VARCHAR(60),
    error_message TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_invoice_jobs_tenant ON invoice_document_jobs(tenant_id);
CREATE INDEX idx_invoice_jobs_document ON invoice_document_jobs(document_id);
CREATE INDEX idx_invoice_jobs_status ON invoice_document_jobs(status) WHERE status IN ('QUEUED', 'RUNNING');

-- Source: V5__tenant_quotas.sql
CREATE TABLE tenant_quotas (
    tenant_id UUID PRIMARY KEY REFERENCES tenants(id),
    uploads_this_month INT NOT NULL DEFAULT 0,
    extractions_this_month INT NOT NULL DEFAULT 0,
    reset_at TIMESTAMPTZ NOT NULL DEFAULT (now() + interval '1 month'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Source: V6__auth_profile_columns.sql
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS tenant_type VARCHAR(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tenant ON refresh_tokens(tenant_id);
CREATE INDEX idx_refresh_expires ON refresh_tokens(expires_at);

-- Flyway baseline marker
CREATE TABLE public.flyway_schema_history (
 installed_rank integer NOT NULL PRIMARY KEY, version varchar(50),
 description varchar(200) NOT NULL, type varchar(20) NOT NULL,
 script varchar(1000) NOT NULL, checksum integer, installed_by varchar(100) NOT NULL,
 installed_on timestamp NOT NULL DEFAULT now(), execution_time integer NOT NULL,
 success boolean NOT NULL
);
CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history(success);
INSERT INTO public.flyway_schema_history
 (installed_rank, version, description, type, script, installed_by, execution_time, success)
 VALUES (1, '6', 'Local schema snapshot V1-V6', 'BASELINE', 'Local schema snapshot V1-V6', current_user, 0, true);
COMMIT;