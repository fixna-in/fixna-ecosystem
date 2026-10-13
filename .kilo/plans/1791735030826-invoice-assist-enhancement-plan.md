# Invoice Assist — Secure Private Intake & Review Enhancement Plan

## Context
The Consulting API already has a working invoice document intake pipeline (ADR-011, ADR-012):
- Private PDF upload with tenant-scoped storage, deduplication, size limits
- Native PDF text extraction (PDFBox) + OCR fallback interface
- Deterministic field extraction with provenance & confidence scoring
- MoneyGrammar for exact decimal parsing (scale ≤ 2)
- Confidence gates (AUTOFILL ≥0.90, NEEDS_REVIEW 0.70–0.89, HIDDEN <0.70)
- Human review workflow: confirm → approve → post to draft invoice
- CSV export with audit trail
- Retention job (180-day purge with tombstones)

**Gaps to address per requirements:**
1. **Async extraction jobs** — currently synchronous in request path
2. **Malware scanning** — not implemented
3. **Page count limits** — not enforced
4. **Signed/short-lived download URLs** — direct byte streaming only
5. **Search across extractions** — no query endpoint
6. **Provider processing terms review** — no external provider integration guard
7. **Quota management** — per-tenant upload/extraction quotas
7. **Audit log hygiene** — ensure no document content leaks
8. **Comprehensive test coverage** for edge cases listed in requirements

---

## Design Decisions (Resolved)

### 1. Async Extraction Job Model
- **New entity**: `InvoiceDocumentJob` (status: QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED)
- **Queue**: DB-backed polling (existing `InvoiceRetentionJob` pattern) — no new infra
- **Trigger**: `POST /extract` returns `202 Accepted` with `jobId`; client polls `GET /jobs/{jobId}`
- **Worker**: `@Scheduled` fixed-rate (e.g., every 10s) picks QUEUED jobs, runs extraction, updates status
- **Failure**: Job stores error code/message; extraction status stays `NEEDS_REVIEW` or `FAILED`; review/export still works on partial results

### 2. Malware Scanning
- **Interface**: `MalwareScanner` with `scan(byte[]) -> ScanResult { CLEAN, INFECTED, ERROR }`
- **Default**: `NoopMalwareScanner` (always CLEAN) — real engine (ClamAV) can be swapped via bean
- **Integration**: Called in `upload()` before storage; on INFECTED → reject upload, audit, no bytes persisted
- **Config**: `fixna.malware-scanner.enabled=true` to activate real scanner

### 3. Page Count Limits
- **Config**: `fixna.invoice.max-pages=50` (default)
- **Enforcement**: In `PdfTextExtractor.extract()` — throw `PAGE_LIMIT_EXCEEDED` before OCR fallback
- **Applies to**: Both native and OCR pages (total)

### 4. Signed Download URLs
- **New endpoint**: `POST /invoices/{invoiceId}/documents/{documentId}/download-url`
- **Returns**: `{ url: string, expiresAt: OffsetDateTime }` — pre-signed URL valid 15 min (configurable)
- **Implementation**: `FileStorage.generatePresignedUrl(tenantId, storageKey, ttl)` — S3-compatible or local proxy
- **Fallback**: Local storage returns `/api/v1/.../content?token=...` validated by short-lived JWT claim

### 5. Search Across Extractions
- **New endpoint**: `GET /api/v1/invoice-extractions/search`
- **Params**: `q` (field_key/value text), `status`, `dateFrom`, `dateTo`, `currency`, `page`, `size`
- **Response**: Paged `InvoiceExtractionSummary` (id, documentId, invoiceId, status, createdAt, fieldCount, totalAmount)
- **Index**: Add composite index on `(tenant_id, created_at DESC)` + GIN on `field_value` if needed

### 6. Provider Processing Terms Guard
- **New config**: `fixna.ai.document-processing.allowed-providers=[]` (empty = none allowed)
- **Check**: Before any AI call that sends document text, verify provider is allowlisted
- **Audit**: Log provider name, documentId, decision (allowed/blocked) — never document content
- **Fail-open for review**: If provider blocked, extraction still proceeds with native/OCR only; AI suggestions skipped

### 7. Quota Management
- **New table**: `tenant_quotas (tenant_id PK, uploads_this_month, extractions_this_month, reset_at)`
- **Limits**: Configurable via `fixna.invoice.quotas.monthly-uploads=100`, `.monthly-extractions=200`
- **Enforcement**: Checked in `upload()` and `extract()` (or job queue); returns `QUOTA_EXCEEDED` 429
- **Reset**: Monthly cron job (or on first request after `reset_at`)

### 8. Audit Log Hygiene
- **Rule**: Never log `field_value`, `raw_snippet`, `money_amount`, `currency`, OCR text, bank details, tax IDs
- **Review**: Existing audit calls in `InvoiceDocumentService` — only log IDs, counts, status transitions
- **Add**: Sanitization utility for any future audit payloads

### 9. Test Coverage Additions
| Scenario | Test Location |
|----------|---------------|
| Malformed PDF (encrypted, corrupt, non-PDF) | `PdfTextExtractorTest` |
| OCR unavailable / OCR failure | `InvoiceDocumentServiceTest` (mock OcrEngine) |
| Decimal edge cases (scale>2, thousands separators, currency symbols) | `MoneyGrammarTest` |
| Date parsing (ISO, DD/MM/YYYY, MM/DD/YYYY) | `FieldExtractorTest` |
| Uncertainty gates (confidence boundaries, monetary parse fail) | `ConfidenceGateTest` |
| Review authorization (cross-tenant, wrong role) | `InvoiceDocumentServiceTest` |
| Signed URL expiry / reuse | `InvoiceDocumentControllerTest` |
| Hard delete + retention job | `InvoiceRetentionJobTest` |
| CSV escaping (commas, quotes, newlines) | `InvoiceDocumentServiceTest.exportCsv()` |
| Prompt injection in AI suggestions | `AiProviderTest` (mock) |
| Quota exceeded | `InvoiceDocumentServiceTest` |

---

## Implementation Tasks (Ordered)

### Phase 1: Core Infrastructure
- [ ] **T1** Add `InvoiceDocumentJob` entity + repository + migration V14
- [ ] **T2** Add `MalwareScanner` interface + `NoopMalwareScanner` bean
- [ ] **T3** Add page limit config + enforcement in `PdfTextExtractor`
- [ ] **T4** Add quota table + repository + monthly reset job + enforcement in service
- [ ] **T5** Add signed URL generation to `FileStorage` (S3 + local proxy implementations)

### Phase 2: Async Extraction Pipeline
- [ ] **T6** Refactor `InvoiceDocumentService.extract()` → enqueue job, return 202
- [ ] **T7** Create `InvoiceDocumentJobWorker` (@Scheduled) that processes QUEUED jobs
- [ ] **T8** Add `GET /api/v1/invoice-jobs/{jobId}` poll endpoint + controller
- [ ] **T9** Job stores error details; partial results still queryable via extraction endpoint

### Phase 3: Search & Download UX
- [ ] **T10** Add search endpoint + repository query method + controller
- [ ] **T11** Add download-url endpoint + controller + `FileStorage` impl
- [ ] **T12** Update frontend (consulting-web) to use async poll + signed URLs

### Phase 4: Provider Guard & Hygiene
- [ ] **T13** Add provider allowlist config + check in `suggestLineIdeas()` and any future AI document calls
- [ ] **T14** Audit log review — add sanitization utility + verify no content leaks
- [ ] **T15** Add malware scan call in `upload()` before storage

### Phase 5: Test Coverage
- [ ] **T16** Unit tests: `MoneyGrammarTest`, `ConfidenceGateTest`, `FieldExtractorTest`
- [ ] **T17** Integration tests: `InvoiceDocumentServiceTest` (upload, extract async, approve, post, export, delete, quota, cross-tenant)
- [ ] **T18** Controller tests: `InvoiceDocumentControllerTest` (signed URL, search, job polling)
- [ ] **T19** Retention job test: `InvoiceRetentionJobTest`
- [ ] **T20** OCR engine test: `NoopOcrEngineTest` + mock OCR failure scenarios

### Phase 6: Documentation & Config
- [ ] **T21** Update `application.yml` with new config keys (page limit, quotas, malware, provider allowlist, signed URL TTL)
- [ ] **T22** Update `docs/specs/invoice-assistant-api-contract.md` with new endpoints
- [ ] **T23** Update `docs/specs/invoice-assistant-database-design.md` with job/quota tables
- [ ] **T24** Add ADR for async job model + signed URLs

---

## Configuration Keys (New)

```yaml
fixna:
  invoice:
    max-pages: 50
    retention-cron: "0 30 3 * * *"
    quotas:
      monthly-uploads: 100
      monthly-extractions: 200
    signed-url-ttl-minutes: 15
  malware-scanner:
    enabled: false
  ai:
    document-processing:
      allowed-providers: []  # e.g., ["openai", "anthropic"]
```

---

## Database Migrations (New)

**V14__invoice_document_jobs.sql**
```sql
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
```

**V15__tenant_quotas.sql**
```sql
CREATE TABLE tenant_quotas (
    tenant_id UUID PRIMARY KEY REFERENCES tenants(id),
    uploads_this_month INT NOT NULL DEFAULT 0,
    extractions_this_month INT NOT NULL DEFAULT 0,
    reset_at TIMESTAMPTZ NOT NULL DEFAULT (now() + interval '1 month'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## Failure Modes & Rollback

| Failure | Behavior | Rollback |
|---------|----------|----------|
| Malware scan INFECTED | Reject upload (400), no storage write, audit `malware_detected` | N/A (no state change) |
| Page limit exceeded | Reject extraction (400), job FAILED, audit | N/A |
| OCR engine unavailable | Native-only extraction; if no native text → job FAILED with `OCR_UNAVAILABLE` | Job retryable |
| AI provider not allowlisted | Skip AI suggestions; native/OCR extraction proceeds | N/A |
| Quota exceeded | 429 on upload/extract; job not queued | N/A |
| Signed URL expired | 410 on download attempt | Client re-requests |
| Cross-tenant access | 403/404 via existing tenant checks | N/A |
| CSV injection attempt | Escaped by `csvCell()`; audit `export.generated` | N/A |

---

## Validation Plan

1. **Run existing tests**: `npm run test:consulting-api` — must stay green
2. **Run new unit tests**: `mvn -f backend/consulting-api/pom.xml test -Dtest=*MoneyGrammar*,*ConfidenceGate*,*FieldExtractor*`
3. **Run integration tests**: `mvn -f backend/consulting-api/pom.xml test -Dtest=InvoiceDocumentServiceTest`
4. **Run controller tests**: `mvn -f backend/consulting-api/pom.xml test -Dtest=InvoiceDocumentControllerTest`
5. **Run retention job test**: `mvn -f backend/consulting-api/pom.xml test -Dtest=InvoiceRetentionJobTest`
6. **Manual smoke**: Upload PDF → poll job → review fields → approve → post → export CSV → delete → verify purge

---

## Open Questions (None — all resolved above)

---

## Out of Scope
- Real ClamAV integration (bean swap only)
- S3-compatible storage implementation (local proxy sufficient for MVP)
- AI-powered field extraction (current deterministic + advisory AI only)
- Multi-page OCR coordination (handled per-page in PdfTextExtractor)
- Webhook/callback on job completion (polling only)