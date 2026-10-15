# Database Design — Invoice Assistant intake (proposed, NOT applied)

⚠️ **Design-only.** No Flyway file from this document; implementation requires its own reviewed migration (V12+ namespace; exact version assigned at implementation time). V11 `invoice_assist_suggestions` unchanged; no ALTERs anywhere; no cross-product tables (ADR-008, D12).

## Proposed: `invoice_documents`

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | opaque document id — never exposes the storage key |
| tenant_id | UUID NOT NULL | FK tenants |
| invoice_id | UUID NOT NULL | FK invoices CASCADE |
| file_name | TEXT NOT NULL | original, sanitized for display |
| storage_key | TEXT NOT NULL | FileStorage-internal; never serialized to clients |
| content_type | VARCHAR(100) NOT NULL | `application/pdf` |
| size_bytes | BIGINT NOT NULL CHECK (size_bytes > 0) | ≤ 10 MB enforced in the service |
| sha256 | VARCHAR(64) NOT NULL | forensics + dedupe |
| status | VARCHAR(30) NOT NULL | RECEIVED/NEEDS_REVIEW/APPROVED/POSTED/REJECTED/DELETED (+ CHECK) |
| uploaded_by_user_id | UUID NOT NULL | FK users |
| purge_after | TIMESTAMPTZ | retention clock (product spec §3.8) |
| created_at / updated_at | TIMESTAMPTZ NOT NULL | |

Indexes: `(tenant_id, invoice_id)`; partial `(tenant_id, purge_after) WHERE status <> 'DELETED'` for the retention scan; `UNIQUE (tenant_id, invoice_id, sha256)`.

## Proposed: `invoice_document_extractions`

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | extraction run id |
| tenant_id | UUID NOT NULL | FK tenants |
| document_id | UUID NOT NULL | FK invoice_documents CASCADE |
| status | VARCHAR(30) NOT NULL | NEEDS_REVIEW/APPROVED/REJECTED/POSTED |
| method_native_pages | INT NOT NULL DEFAULT 0 | pipeline-mix observability |
| method_ocr_pages | INT NOT NULL DEFAULT 0 | |
| extractor_version | VARCHAR(40) NOT NULL | e.g. `native-1`, `ocr-tess-5` |
| approved_by_user_id | UUID | FK users, nullable |
| approved_at | TIMESTAMPTZ | |
| posted_invoice_id | UUID | FK invoices ON DELETE SET NULL |
| created_at / updated_at | TIMESTAMPTZ NOT NULL | |

## Proposed: `invoice_document_fields`

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| tenant_id | UUID NOT NULL | FK tenants |
| extraction_id | UUID NOT NULL | FK invoice_document_extractions CASCADE |
| field_key | VARCHAR(60) NOT NULL | vendor_name, invoice_number, invoice_date, due_date, total, currency, line.N.description… |
| field_value | TEXT | exact extracted string |
| money_amount | NUMERIC(14,2) | nullable; **exact decimal only — never float** |
| currency | CHAR(3) | ISO-4217 when monetary |
| confidence | NUMERIC(3,2) NOT NULL CHECK (confidence >= 0 AND confidence <= 1) | |
| source | VARCHAR(20) NOT NULL | NATIVE_TEXT/OCR/AI_ADVISORY/MANUAL (+ CHECK) |
| page | INT | |
| bbox | JSONB | `[x1,y1,x2,y2]`, nullable |
| raw_snippet | TEXT | ≤ 500 chars — human-verifiable receipt |
| extractor_version | VARCHAR(40) NOT NULL | |
| confirmed_by_user_id | UUID | set when a human confirms/edits |

Indexes: `(extraction_id, field_key)`; `(tenant_id)`.

## Invariants

- `money_amount` is set **iff** the field parses via the exact decimal grammar; the service rejects imprecise values before persist (ADR-012).
- `AI_ADVISORY` rows can never be the sole basis for a gate (service rule).
- Deleting a document cascades extractions/fields in one transaction (hard delete); the `audit_logs` tombstone is id-only.
- No FK or logical link between `invoiceassist` and `websiteaudit` domains.
- All repository methods are `findBy*AndTenantId`-scoped (existing convention).
