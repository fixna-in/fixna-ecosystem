-- LOCAL ONLY. Run with psql -X -h localhost -U postgres -d website_audit -f schema.sql
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

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID REFERENCES users(id),
    event VARCHAR(100) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    details JSONB,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_tenant_time ON notifications(tenant_id, created_at);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id),
    permission_id UUID NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    tenant_id UUID REFERENCES tenants(id),
    PRIMARY KEY (user_id, role_id, tenant_id)
);

-- Source: V2__website_audits.sql
CREATE TABLE website_audits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    input_url TEXT NOT NULL,
    normalized_url_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    overall_score INTEGER NOT NULL DEFAULT 0 CHECK (overall_score >= 0 AND overall_score <= 100),
    summary TEXT,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_website_audits_tenant ON website_audits(tenant_id);
CREATE INDEX idx_website_audits_tenant_created ON website_audits(tenant_id, created_at DESC);

CREATE TABLE website_audit_checks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    audit_id UUID NOT NULL REFERENCES website_audits(id) ON DELETE CASCADE,
    check_code VARCHAR(80) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    score INTEGER NOT NULL DEFAULT 0 CHECK (score >= 0 AND score <= 100),
    evidence JSONB NOT NULL DEFAULT '{}'::jsonb,
    fix_hint TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_website_audit_checks_audit ON website_audit_checks(audit_id);
CREATE INDEX idx_website_audit_checks_tenant ON website_audit_checks(tenant_id);

-- Source: V3__website_audit_jobs.sql
CREATE TABLE website_audit_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    audit_id UUID REFERENCES website_audits(id) ON DELETE SET NULL,
    requested_url TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    crawl_enabled BOOLEAN NOT NULL DEFAULT true,
    attempt INT NOT NULL DEFAULT 0,
    locked_by VARCHAR(100),
    lease_expires_at TIMESTAMPTZ,
    failure_code VARCHAR(60),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_website_audit_jobs_claim ON website_audit_jobs(status, lease_expires_at);
CREATE INDEX idx_website_audit_jobs_tenant_created ON website_audit_jobs(tenant_id, created_at DESC);
CREATE UNIQUE INDEX uq_website_audit_jobs_one_running
    ON website_audit_jobs(tenant_id) WHERE status = 'RUNNING';

CREATE TABLE website_audit_pages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    audit_id UUID NOT NULL REFERENCES website_audits(id) ON DELETE CASCADE,
    job_id UUID REFERENCES website_audit_jobs(id) ON DELETE SET NULL,
    url TEXT NOT NULL,
    url_hash VARCHAR(64) NOT NULL,
    depth INT NOT NULL DEFAULT 0,
    http_status INT,
    content_type VARCHAR(100),
    size_bytes INT,
    fetch_ms INT,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (audit_id, url_hash)
);

CREATE INDEX idx_website_audit_pages_audit ON website_audit_pages(audit_id);
CREATE INDEX idx_website_audit_pages_tenant ON website_audit_pages(tenant_id);

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
 VALUES (1, '3', 'Local schema snapshot V1-V3', 'BASELINE', 'Local schema snapshot V1-V3', current_user, 0, true);
COMMIT;