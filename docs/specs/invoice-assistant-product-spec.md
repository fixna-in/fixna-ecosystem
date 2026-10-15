# Product Specification — Invoice Assistant (Consulting)

Status: Draft for approval (2026-10-12). Scope: Consulting only. Target design; shipped MVP summarized first. No implementation implied.

## 1. Purpose

Consultants receive supplier PDFs; the assistant turns them into reviewed, verifiable invoice data — with humans in control of money at all times.

## 2. Current state (shipped, V11)

- Deterministic suggestions on existing invoice rows: MISSING_LINES, DUE_DATE_RECOMMENDED (draft-only auto-apply), OVERDUE_REMINDER; advisory AI LINE_IDEAS.
- Endpoints: `GET/POST /invoices/{id}/assist/suggestions`, `POST /assist/suggestions/{id}/apply|dismiss`; `INVOICE_ASSIST_USE` (+ `AI_USE` for ideas).
- Table `invoice_assist_suggestions`; open suggestions de-duplicated per type.

## 3. Target scope

### 3.1 Private upload (ADR-011)

PDF only, ≤ 10 MB, magic-byte validated, requires `INVOICE_MANAGE`; stored via existing `FileStorage` (private, tenant-partitioned); opaque document id; authenticated streaming download only; no public URLs, ever.

### 3.2 Text & OCR

Native PDF text layer first; OCR adapter fallback (offline-capable default engine) when native text is thin (< 200 chars/page); per-page extraction records; optional AI structuring is advisory only.

### 3.3 Extraction provenance

Every field carries `{source: NATIVE_TEXT|OCR|AI_ADVISORY|MANUAL, page, bbox?, rawSnippet ≤ 500 chars, extractorVersion}`. The UI shows the snippet beside each value so a human can verify origin.

### 3.4 Confidence handling

Field confidence 0–1; gates: ≥ 0.90 auto-prefill / 0.70–0.89 review-required / < 0.70 hidden (ADR-012). Money: exact BigDecimal grammar, scale ≤ 2; failed parse → rejected field. A Σ-lines-vs-total cross-check produces an advisory flag only — it never bypasses or replaces the gates.

### 3.5 Human review

State machine `DRAFT → NEEDS_REVIEW → APPROVED → POSTED` (| `REJECTED`); server-enforced transitions (`409 INVALID_STATE`); the reviewer may edit any field; every change is audited with the actor.

### 3.6 Approval & posting

`INVOICE_MANAGE` + audit event; single-approver MVP (four-eyes reserved, IA-11); posting patches only `DRAFT` invoices; posted values retain provenance refs; AI never approves or posts.

### 3.7 Export

Per-document export: CSV (flat fields + provenance columns) and JSON snapshot (full fidelity), generated on demand via an authenticated endpoint; the export action itself is audited (`invoice.export.generated`).

### 3.8 Retention & deletion

Raw upload + derived text + provenance purged 180 days after the invoice reaches POSTED/VOID or the document is deleted, whichever first (global default now; per-tenant configurability reserved). Deletion: hard delete of file/text/provenance; id-only tombstone in `audit_logs`. Users may delete at any time regardless of the retention clock.

### 3.9 AI-optional

Upload → extract → gates → review → approve → post → export → retention all function with `AiProvider` unavailable. Advisory AI endpoints return `503 AI_UNAVAILABLE`; the UI degrades gracefully.

## 4. Non-goals

No auto-posting without human approval; no bank/card data capture (card-like fields are redacted at extraction); no currency conversion; non-PDF formats rejected with `UNSUPPORTED_MEDIA_TYPE`.

## 5. Permissions

`INVOICE_MANAGE` (upload/download/review/approve/post/delete/export), `INVOICE_ASSIST_USE` (existing suggestions), `AI_USE` (advisory). No new permissions (D11).

## 6. Acceptance summary

- Uploaded bytes are never reachable without JWT + tenant match; path traversal impossible (opaque ids, server-generated storage keys).
- Every displayed value has provenance + confidence.
- No monetary value enters an invoice without an exact BigDecimal parse **and** human approval.
- The full flow passes with the AI provider disabled.
