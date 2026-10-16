-- V4: AI usage log for advisory fixes (POST /website-audits/{id}/fixes).
-- Spec: docs/specs/website-audit-product-spec.md §3.5 + website-audit-database-design.md.
-- The AiUsageLog entity (in.fixna.websiteaudit.ai) is validated at startup
-- (ddl-auto: validate), so the table must exist. Advisory-only: stores
-- tenant/user scope, feature enum, and a short request summary — never
-- prompts, keys, tokens, or fetched page content.
CREATE TABLE ai_usage_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id),
    feature VARCHAR(50) NOT NULL,
    request_summary TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_usage_logs_tenant_created ON ai_usage_logs(tenant_id, created_at);
