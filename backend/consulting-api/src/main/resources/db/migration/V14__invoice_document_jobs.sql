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