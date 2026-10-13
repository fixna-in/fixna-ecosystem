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