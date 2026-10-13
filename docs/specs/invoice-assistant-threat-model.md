# Threat Model — Invoice Assistant (Consulting)

Assets: uploaded financial documents (PII), invoice integrity, tenant boundary, AI spend, storage/disk.
Trust: the authenticated consultant is trusted-with-boundaries; uploaded document contents and AI output are **hostile input**. In the MVP actor model, uploader = reviewer.

| # | Threat | Impact | Mitigations | Residual |
|---|--------|--------|-------------|----------|
| I1 | Malicious PDF (parser exploit, xref bomb, huge streams) | Critical | PDF-only magic bytes; ≤ 10 MB; parse under timeout + memory cap; per-document failure isolation; parser patch cadence (IA-08) | Medium — parser CVEs |
| I2 | Stored path traversal / file overwrite | High | Opaque UUID ids; server-generated `storage_key` via `FileStorage`; client filename sanitized for display only, never used in paths | Low |
| I3 | Cross-tenant document/extraction read (id guessing) | High | UUID ids + `findBy*AndTenantId`; 404 on mismatch; download streams only after the tenant check | Low |
| I4 | Unauthenticated/public exposure of financial documents | Critical | No public URLs, no presigned links; JWT-only streaming endpoint; `SecurityHeadersFilter`; never logged | Low |
| I5 | PII/secrets leaking into logs or AI prompts | High | Raw snippets capped; `SensitiveDataMasker`; AI advisory receives a redacted field set (card/bank-like fields redacted at extraction); `request_summary` only — never full text | Low |
| I6 | Decimal fraud (float rounding, locale tricks, scale abuse) | Critical | Exact BigDecimal grammar + `NUMERIC(14,2)`; scale ≤ 2; Σ-lines-vs-total advisory flag; human approval gates (ADR-012); AI can never set `money_amount` | Low |
| I7 | OCR spoofing (tiny/hidden text, whitespace lattice) | Medium | Per-field confidence; low-confidence hidden; bbox + snippet shown for human verification; approval required regardless | Medium — human vigilance |
| I8 | Prompt injection via document text into AI advisory | Medium | Advisory-only channel; schema-validated AI output; never auto-applied (existing rule); AI has no tool access | Low |
| I9 | Retention creep (disk DoS, GDPR breach surface) | Medium | `purge_after` column + purge job (IA-09); user-initiated delete anytime; deletion removes bytes; tombstone is id-only | Medium until purge ships |
| I10 | Review bypass (approve without clearing gates) | High | Server-side gate check at approve (`409 GATES_UNMET`); server-enforced state machine; audit event with actor | Low |
| I11 | Duplicate posting onto invoices | Medium | POSTED append-only; posting only to `DRAFT` invoices; `UNIQUE (tenant, invoice, sha256)` | Low |

## Review cadence

Re-threat-model when: OCR moves to a cloud API (new cross-border data flow), non-PDF formats are allowed, four-eyes approval changes the actor model, or documents become shareable with client-portal users.
