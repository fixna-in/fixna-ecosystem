# Product Specification — Website Audit (Consulting)

Status: Draft for approval (2026-10-12). Scope: Consulting product only. This spec defines the **target design**; the shipped MVP is summarized first. No implementation is implied by this document.

## 1. Purpose

Consultants audit client websites and leave with an actionable, evidence-backed report. AI improves suggestions but is never required.

## 2. Current state (shipped, V10)

- Single-page audit: `POST /api/v1/website-audits` (synchronous), list/detail, advisory `POST /{id}/fixes`.
- Fetcher: http/https, ports 80/443, DNS-range validation, no redirects, 512 KB cap, hard timeouts.
- 14 deterministic checks (HTTP status, HTTPS, title, meta description, viewport, Open Graph, canonical, H1, img alt, charset, lang, response time, robots.txt, sitemap.xml); overall score = rounded average; HTML checks are skipped (not failed) for non-HTML or non-2xx responses.
- AI fixes advisory only (`WEBSITE_AUDIT_MANAGE` + `AI_USE`), logged to `ai_usage_logs`.
- Tables: `website_audits`, `website_audit_checks` (V10). 21 unit tests green.

## 3. Target scope

### 3.1 Bounded crawling (ADR-009)

Seed URL → same registrable domain only; depth ≤ 2; ≤ 25 stored pages; ≤ 40 fetch attempts; 60s wall-clock per job attempt; 500ms per-host delay; robots.txt honored (crawl-delay cap 5s); one RUNNING crawl per tenant. Bounds are server-enforced and clamped regardless of client input. Per-page results persist as crawl progress (`website_audit_pages`, design-only) enabling partial reports and idempotent recovery.

### 3.2 Async jobs & recovery (ADR-009)

202 + poll; states QUEUED/RUNNING/COMPLETED/FAILED/CANCELLED; 90s lease with heartbeat; startup requeue of expired leases; ≤ 3 attempts then FAILED(`RECOVERY_EXHAUSTED`); cancel allowed while QUEUED or RUNNING.

### 3.3 Deterministic checks & evidence

- The MVP check set applies to the seed page. Crawl adds site-level checks: `internal_links` (count/quality), `duplicate_titles`, `thin_content` (pages < 250 words), `missing_alt_by_page`.
- Every finding stores evidence JSONB `{observed, expected, snippet (≤ 300 chars), page}` — **immutable after job completion**; re-audits create new rows, never update old ones (D4).
- Scoring: per-page score, then site score = weighted mean (seed weight 0.4, other pages 0.6; exact weights live as `WebsiteAuditor` constants so golden tests pin them). AI never writes scores or findings.

### 3.4 Reporting

- JSON report = audit + checks + pages (API, tenant-scoped).
- Human-readable export: single-file self-contained HTML generated server-side (no external assets, CSP meta `default-src 'none'`), downloadable via an authenticated endpoint, branded with tenant name. XSS-escape everything rendered from fetched content.

### 3.5 Usage & observability

- Audit runs: audit events only (`website.audit.queued|started|completed|failed|cancelled`).
- AI fixes: `ai_usage_logs` + `ai.suggestion.generated` (existing).
- Structured lifecycle logging on `fixna.jobs` + `fixna.audit` with MDC tenant/user/request ids.

### 3.6 AI-optional

Queue → crawl → checks → score → report all run with `AiProvider` unavailable. `/fixes` returns `FixnaException("AI_UNAVAILABLE", 503, ...)`; the UI hides/disables the advisory panel.

## 4. Non-goals

No authenticated-page crawling, no browser/JS rendering, no screenshots, no competitor scans, no cross-tenant URL deduplication, no email dispatch.

## 5. Permissions

`WEBSITE_AUDIT_VIEW` (list/detail/export), `WEBSITE_AUDIT_MANAGE` (submit/cancel/fixes). No new permissions (D11).

## 6. Acceptance summary

- Bounds enforced server-side even when the client lies (params clamped).
- Kill/restart mid-crawl → the job resumes without duplicate pages and completes or fails cleanly within 3 attempts.
- All checks reproducible: identical fetched content → identical scores (golden tests).
- Nothing in the flow requires AI.
