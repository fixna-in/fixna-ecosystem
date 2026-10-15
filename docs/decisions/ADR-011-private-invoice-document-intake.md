# ADR-011: Private Invoice Document Intake (Storage, Text/OCR, Provenance)

## Status

Proposed (2026-10-12) — pending approval ([decision register](../specs/README.md))

## Context

The Invoice Assistant MVP reasons only over invoice rows already in the database. To assist with real supplier invoices, users must be able to attach a PDF. Invoices carry financial PII: uploads must be private, tenant-isolated, and malware-resistant, and text extraction must record where each value came from so humans can verify it. No AI or cloud OCR dependency may be required for the core path.

## Decision

1. **Upload:** `POST /invoices/{id}/documents` — PDF only (`%PDF-` magic byte), ≤ 10 MB, requires `INVOICE_MANAGE`. Stored via the existing `FileStorage.store(tenantId, ...)` → tenant-partitioned private path. **No public URLs, ever** — no presigned links, no CDN exposure.
2. **Download:** authenticated streaming endpoint via `FileStorage.open(tenantId, storageKey)`; the storage key is never returned to clients — clients see an opaque document UUID only.
3. **Extraction pipeline (deterministic-first):**
   a. **Native PDF text layer** — primary path.
   b. **OCR adapter** — fallback when native text is thin (< 200 chars/page); the adapter is a pluggable interface whose default engine must run offline/on-prem.
   c. **AI structuring** — optional and advisory only; extraction completes fully with `AiProvider` unavailable.
4. **Provenance:** every extracted field row stores `{source: NATIVE_TEXT | OCR | AI_ADVISORY, page, boundingBox (nullable), rawSnippet (≤ 500 chars), extractorVersion}`. The raw snippet is the human-verifiable receipt shown beside the value.
5. **Confidence:** field-level score 0.00–1.00 produced by deterministic parsers (regex/anchor matching). AI-sourced values carry `AI_ADVISORY` and can never auto-satisfy a gate (ADR-012).
6. **Money:** amounts parse only through an exact decimal grammar (digits, locale-aware separators from a fixed set, ≤ 2 fraction digits) into `BigDecimal`; anything imprecise is flagged, never coerced. No double/float anywhere.
7. **Retention:** raw upload + derived text + provenance are purged 180 days after the invoice reaches POSTED/VOID or the document is deleted, whichever comes first (configurable; the purge worker is a backlog task — no scheduler exists yet).
8. **Deletion:** hard delete of file + extracted text + provenance rows; `audit_logs` keeps an id-only tombstone event (no PII payloads). Users may delete at any time regardless of the retention clock.

## Consequences

- Reuses `FileStorage` unchanged; zero shared-lib changes.
- New tables (`invoice_documents`, `invoice_document_extractions`, `invoice_document_fields`) are consulting-only and Flyway-gated at implementation time.
- The OCR engine choice is deferred behind the adapter interface (backlog IA-04).
- Retention purge requires introducing the first scheduled worker in consulting-api — explicitly scoped and isolated (backlog IA-09).
