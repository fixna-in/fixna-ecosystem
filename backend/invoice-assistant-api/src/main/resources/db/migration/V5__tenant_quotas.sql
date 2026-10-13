CREATE TABLE tenant_quotas (
    tenant_id UUID PRIMARY KEY REFERENCES tenants(id),
    uploads_this_month INT NOT NULL DEFAULT 0,
    extractions_this_month INT NOT NULL DEFAULT 0,
    reset_at TIMESTAMPTZ NOT NULL DEFAULT (now() + interval '1 month'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
