-- V2: standalone invoice records owned by Invoice Assistant (no consulting FKs).
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
