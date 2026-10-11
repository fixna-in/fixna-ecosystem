# Implementation Status — Fixna Ecosystem (2026-10-09)

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

## Hospitality — placeholder only (dirs + README-level). Keep marked
planned/coming-soon everywhere. Out of scope for code changes.

## Shared platform (fixna-platform-common 1.0.0)

Request IDs, CORS, security headers, global error envelope, password config.
7 tests. Used by both APIs via Maven (bootstrap:maven installs to .m2).
Note: rate limiting lives per-service (localboost-api original,
consulting-api copy); promote to this lib when a third consumer appears.
