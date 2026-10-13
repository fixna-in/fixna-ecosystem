CREATE TABLE invoice_assist_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    suggestion_type VARCHAR(60) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(30) NOT NULL DEFAULT 'SUGGESTED',
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoice_assist_tenant ON invoice_assist_suggestions(tenant_id);
CREATE INDEX idx_invoice_assist_invoice ON invoice_assist_suggestions(invoice_id);
CREATE INDEX idx_invoice_assist_tenant_created ON invoice_assist_suggestions(tenant_id, created_at DESC);
