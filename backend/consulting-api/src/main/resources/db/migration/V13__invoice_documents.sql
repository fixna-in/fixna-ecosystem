-- V13: private invoice document intake with provenance (ADR-011, ADR-012).
-- Design: docs/specs/invoice-assistant-database-design.md (approved).

CREATE TABLE invoice_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
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

CREATE INDEX idx_invoice_documents_tenant_invoice ON invoice_documents(tenant_id, invoice_id);
CREATE UNIQUE INDEX uq_invoice_documents_dedupe ON invoice_documents(tenant_id, invoice_id, sha256);
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
    posted_invoice_id UUID REFERENCES invoices(id) ON DELETE SET NULL,
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