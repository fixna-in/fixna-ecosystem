# Consulting Feature Specifications — Website Audit & Invoice Assistant

Specification pack for the Consulting product only. **No product features are implemented by these documents**; proposed DDL is design-only and must not be applied without approval. LocalBoost is out of scope (ADR-008: separate databases, never merged migrations).

## Contents

| Document | Purpose |
|---|---|
| [shared-platform-contract.md](shared-platform-contract.md) | Identity, tenant, usage, entitlement, observability — compatibility with existing contracts only |
| [website-audit-product-spec.md](website-audit-product-spec.md) | Bounded crawling, deterministic checks, evidence, reporting, usage, job recovery |
| [website-audit-api-contract.md](website-audit-api-contract.md) | Current endpoints (as shipped) + proposed async job API |
| [website-audit-database-design.md](website-audit-database-design.md) | Proposed tables (design-only, NOT applied) |
| [website-audit-threat-model.md](website-audit-threat-model.md) | SSRF, DNS rebinding, crawl abuse, evidence handling |
| [invoice-assistant-product-spec.md](invoice-assistant-product-spec.md) | Private upload, PDF text/OCR, provenance, confidence, review, approval, export, retention, deletion |
| [invoice-assistant-api-contract.md](invoice-assistant-api-contract.md) | Proposed intake & review endpoints |
| [invoice-assistant-database-design.md](invoice-assistant-database-design.md) | Proposed tables (design-only, NOT applied) |
| [invoice-assistant-threat-model.md](invoice-assistant-threat-model.md) | Upload abuse, PII, decimal integrity, retention |
| [test-plan.md](test-plan.md) | Combined test plan |
| [backlog.md](backlog.md) | Small-task backlog (WB-xx / IA-xx / INF-xx) |

ADRs: [009](../decisions/ADR-009-bounded-website-crawl-jobs.md) bounded crawl jobs · [010](../decisions/ADR-010-pinned-ip-fetcher.md) pinned-IP fetcher · [011](../decisions/ADR-011-private-invoice-document-intake.md) private invoice intake · [012](../decisions/ADR-012-extraction-confidence-approval.md) confidence & approval gates.

## Current state (shipped, for reference)

- **Website Audit MVP:** single-page deterministic audit (14 checks), SSRF-guarded fetcher (no redirects, 512 KB cap), advisory AI fixes; tables `website_audits`, `website_audit_checks` (V10).
- **Invoice Assistant MVP:** deterministic suggestions on invoice data (MISSING_LINES, DUE_DATE_RECOMMENDED — draft-only auto-apply, OVERDUE_REMINDER) + advisory AI LINE_IDEAS; table `invoice_assist_suggestions` (V11).
- Core workflows are AI-optional today; this pack keeps that property for every target feature.

## Decision register (for approval)

| # | Decision | Anchor |
|---|----------|--------|
| D1 | Website audit runs become async jobs (202 + poll) with startup lease recovery | ADR-009 |
| D2 | Crawl bounds: ≤25 pages, depth ≤2, same registrable domain, robots.txt, 60s/40-fetch budget, 500ms per-host delay, 1 RUNNING crawl/tenant | ADR-009 |
| D3 | DNS rebinding defense: resolve-once, validate all A/AAAA, connect to pinned IP with Host+SNI; redirects stay disabled | ADR-010 |
| D4 | Findings immutable & evidence-backed; AI never writes scores or findings | WA product spec |
| D5 | Invoice intake: PDF-only ≤10 MB, magic-byte check, private FileStorage, authenticated streaming download only | ADR-011 |
| D6 | Layered extraction: native PDF text → OCR adapter (offline-capable); field-level provenance with raw snippet | ADR-011 |
| D7 | Confidence gates ≥0.90 auto-prefill / 0.70–0.89 review / <0.70 hidden; money = exact BigDecimal, scale ≤2 | ADR-012 |
| D8 | Workflow DRAFT→NEEDS_REVIEW→APPROVED→POSTED; approval = INVOICE_MANAGE + audit event; single-approver MVP; AI never approves/posts | ADR-012 |
| D9 | Export CSV+JSON via authenticated endpoint; raw upload retention 180 days post-posting; hard delete with id-only tombstone | IA product spec |
| D10 | AI-optional: all core flows work with AiProvider unavailable; AI endpoints fail with `AI_UNAVAILABLE` | shared-platform |
| D11 | No shared-lib changes, no new roles/plans/permissions, no cross-product tables; entitlement = RBAC + rate limits | shared-platform |
| D12 | Proposed DDL is design-only (V12+ namespace reserved); nothing applied without a separate reviewed migration | DB designs |
| D13 | One combined test plan for both features | test-plan.md |
| D14 | Backlog tasks sized ≤0.5–1 day, independently reviewable | backlog.md |

## Status

All decisions **Pending approval**. Upon approval, implementation proceeds via backlog tasks; migrations are applied only when their own Flyway file is reviewed and merged.
