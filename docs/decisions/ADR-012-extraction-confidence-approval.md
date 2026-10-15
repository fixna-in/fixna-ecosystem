# ADR-012: Extraction Confidence Gates and Human Approval for Invoice Assistant

## Status

Proposed (2026-10-12) — pending approval ([decision register](../specs/README.md))

## Context

Extracted invoice fields (vendor, dates, totals, line items) are probabilistic. Financial actions — creating or amending invoices — must never trust raw extraction or AI output. Existing shipped contract to preserve: only the deterministic `DUE_DATE_RECOMMENDED` suggestion auto-applies, and only on draft invoices; AI is advisory-only.

## Decision

1. **Field confidence gates** (deterministic, identical thresholds everywhere):
   - ≥ 0.90 — auto-prefill candidate (still shown in review)
   - 0.70 – 0.89 — requires explicit human confirmation
   - < 0.70 — hidden by default; reviewer may opt in to "show low confidence"
   Monetary fields additionally require an exact `BigDecimal` parse (scale ≤ 2); a failed parse rejects the field regardless of confidence.
2. **Review workflow states:** `DRAFT → NEEDS_REVIEW → APPROVED → POSTED`, plus terminal `REJECTED`. The state machine is server-enforced; illegal transitions return `409 INVALID_STATE`.
3. **Approval** requires the `INVOICE_MANAGE` permission, a persisted actor userId, and an audit event (`invoice.review.approved` with `Map<String,String>` details). Single-approver in MVP; four-eyes (approver ≠ uploader) is a post-MVP backlog item (IA-11), not MVP.
4. **Posting:** APPROVED extractions may create/patch invoice lines **only on editable (`DRAFT`) invoices** — reusing the MVP rule (`409 INVOICE_NOT_EDITABLE` otherwise). Every posted value keeps its provenance reference.
5. **AI separation:** AI may propose line ideas/normalization (existing `LINE_IDEAS` advisory pattern) but can never: satisfy a confidence gate, approve, or post. AI fields render with an advisory badge and provenance `AI_ADVISORY`.
6. **Reversal:** POSTED is append-only in this design; corrections go through existing invoice edit flows so the full audit trail is preserved.

## Consequences

- Humans remain the authority for money; identical deterministic gates are fully unit-testable (boundary tests at 0.90/0.70).
- The review UI must surface provenance (page, snippet) next to every field (backlog IA-10).
- Four-eyes approval, maker-checker reporting, and anomaly flags (total ≠ Σ lines beyond advisory) are explicitly reserved in the backlog, not MVP.
