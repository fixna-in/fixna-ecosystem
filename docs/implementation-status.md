# Implementation Status — Fixna Ecosystem (2026-10-15)

## Consulting — MVP v1.0.0 (Phases 1-10, per README)

Real and working: JWT auth + RBAC, tenant-scoped CRM (clients, contacts,
portal memberships), engagements/projects, meetings (validation + TZ-safe
storage), proposals (draft/edit/line items/totals/approve/send), invoices
(draft/edit/lines/numbering/send/cancel/payment recording with BigDecimal),
client portal (login, projects/proposals/invoices/meetings tabs,
approve/reject), public site publish flow (/site/[slug]), testimonials,
service catalog, MockAiProvider advisory suggestions, audit log,
**auth-surface rate limiting (NEW 2026-10-09: /auth/** + /portal/auth/**,
default 20/min/IP)**, 148 tests.

Mock / advisory-only: AI suggestions (templates; prices fabricated — human
review required); payment RECORDING (no gateway); no calendar sync; no email
delivery. All correctly labelled in code; portal/API never claim otherwise.

Gap (NEW finding): the deployed API at api.consulting.fixna.in returns
HTTP 503 (2026-10-09) — Render-side, owner action required.

## LocalBoost — in monorepo; prod still on standalone repo until cutover

Real and working: JWT auth + auth-surface rate limiting, businesses CRUD,
campaigns lifecycle, leads + status transitions, geo targets, dashboard
analytics from real tenant data, demo-metrics seed (honestly labelled),
229 tests incl. CrossTenantSweepTest.

Mock / advisory-only: Google/Meta/WhatsApp platform adapters (labelled on
landing + controller javadoc); AI recommendations advisory-only.

Fixed (2026-10-09): landing HealthProbe no longer shows raw API errors to
visitors — degrades to a neutral status note.

Gap: the deployed API at api.fixna.in returns HTTP 503 (2026-10-09) —
Render-side, owner action required.

## Corporate site (fixna-web) — 11 routes, production-verified build

Present: home, about, capabilities (legacy, canonicalized to /what-we-do),
what-we-do, products, consulting, technology, industries, contact
(Sheets-backed /api/contact + mailto fallback), privacy-policy, terms;
sitemap (all 10 content routes), robots, per-page metadata, honest
product-status notes.

NEW 2026-10-09: Vitest harness (21 tests), typecheck script, CI job,
Google-Sheets contact flow (needs owner env setup — see change log).

NEW 2026-10-15: Products page updated with all 5 live products (LocalBoost, Consulting, Website Audit, Invoice Assistant, Hospitality).

## Website Audit — LIVE at https://websiteaudit.fixna.in

Separate deploy unit per the Website Audit & Invoice Assistant blueprint.
API `backend/website-audit-api` (Spring Boot 3.5.6, port 8082, DB `website_audit`,
Flyway V1–V3) + Web `frontend/website-audit-web` (Next.js 16, port 3003).
Deploy: `render-website-audit.yaml` + `backend/website-audit-api/Dockerfile`
(+ Vercel). Docs: `docs/products/website-audit.md`,
`docs/deployment/website-audit.md`. CI: `website-audit-api` + `website-audit-web` jobs.

Real and working: JWT auth + refresh + `/me`, tenant/RBAC isolation, bounded
crawl with SSRF/DNS-rebinding-guarded pinned-IP fetcher (ADR-009/010),
deterministic checks, async 202 job model (submit/poll/cancel) with worker,
self-contained CSP HTML report, advisory-only AI fixes, auth rate limiting.
Web: `/login`, `/` dashboard (submit+poll+list), `/audits/[id]` (findings,
report download, AI suggestions). 9 API tests + 4 web tests pass; both build.

Fixed to make it build (was scaffolded but never compiled): missing outer-class
imports (WebsiteFetcher/WebsiteAuditor/WebsiteCrawler), malformed HTML-escape
text block in WebsiteAuditReportService, duplicate RateLimitFilter constructor,
and a missing `JwtService.properties()` accessor.

## Invoice Assistant — LIVE at https://invoice.fixna.in

Separate deploy unit. API `backend/invoice-assistant-api` (Spring Boot 3.5.6,
port 8083, DB `invoice_assistant`, Flyway V1–V6) + Web
`frontend/invoice-assistant-web` (Next.js 16, port 3004). Deploy:
`render-invoice-assistant.yaml` + `backend/invoice-assistant-api/Dockerfile`
(+ Vercel). Docs: `docs/products/invoice-assistant.md`,
`docs/deployment/invoice-assistant.md`. CI: `invoice-assistant-api` +
`invoice-assistant-web` jobs.

Real and working: JWT auth + refresh + `/me`, tenant/RBAC isolation, private
PDF intake (ADR-011: PDF-only ≤10MB, magic-byte, sha256, private FileStorage,
authenticated-only `/content`), async extraction job (native text → offline
OCR), provenance + deterministic confidence, confidence gates, review workflow
(confirm → approve/reject/post), CSV/JSON export, search, retention purge,
monthly quotas. Web: `/login`, `/` dashboard (upload/list/delete),
`/documents/[documentId]` (extract, review with confidence badges, edit/confirm,
approve/reject/post, export, download original). 15 API tests + 4 web tests pass;
both build.

Fixed to make it build: package-private `document` utility classes/members
(`ConfidenceGate`, `MoneyGrammar`, `FieldExtractor`, `PdfTextExtractor`) made
public, missing `PdfTextExtractor` import, duplicate RateLimitFilter
constructor, missing `JwtService.properties()` accessor, and a
`ConfidenceGate.blocksApprovalUntilConfirmed` logic bug (hidden <0.70 fields
incorrectly blocked approval — caught by ConfidenceGateTest).

Known limitation (owner action): Render web filesystem is ephemeral — attach a
persistent disk and set `FIXNA_FILE_STORAGE_PATH=/var/data/invoice-files` before
live uploads, or move to object storage behind `FileStorage`.

## Hospitality — LIVE at https://hospitality.fixna.in

Separate deploy unit. API `backend/hospitality-api` (Spring Boot 3.5.6,
port 8084, DB `hospitality`, Flyway) + Web
`frontend/hospitality-web` (Next.js 16, port 3005). Deploy:
`render-hospitality.yaml` + `backend/hospitality-api/Dockerfile`
(+ Vercel). Docs: `docs/products/hospitality.md`,
`docs/deployment/hospitality.md`.

Real and working: JWT auth + refresh + `/me`, tenant/RBAC isolation,
operations, guest experiences, service workflows, business configuration.

## Shared platform (fixna-platform-common 1.0.0)

Request IDs, CORS, security headers, global error envelope, password config.
7 tests. Used by all APIs via Maven (bootstrap:maven installs to .m2).
Note: rate limiting lives per-service (localboost-api original,
consulting-api copy); promote to this lib when a third consumer appears.
