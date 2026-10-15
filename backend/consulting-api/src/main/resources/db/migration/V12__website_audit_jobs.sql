-- V12: async crawl jobs with lease recovery (ADR-009) + per-page crawl progress.
-- Design: docs/specs/website-audit-database-design.md (approved).

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