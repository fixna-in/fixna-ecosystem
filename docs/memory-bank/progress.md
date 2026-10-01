# Progress Log

## 2026-09-28 — Architecture validation corrections

**Delivered:**
- LocalBoost Docker + Render monorepo build (platform-common included)
- Maven bootstrap (`npm run bootstrap:maven`, `scripts/bootstrap-maven.*`)
- Consulting default `POSTGRES_USER=postgres`
- Structured docs: `docs/architecture/`, `docs/products/`, ADR-007/ADR-008
- CI parent POM install; hospitality-web placeholder trimmed

**Verified:** independent API tests, frontend builds, full reactor (372 tests)

## 2026-09-28 — Repo cleanup

**Removed:**
- Stale `services/` folder (lock files and partial artifacts from failed restructure)
- `RESTORE-CONSULTING.md`, `REBUILD-CONSULTING.md` (superseded by memory bank + CHANGELOG)
- `docker-compose.localboost.yml` (redundant; root `docker-compose.yml` covers both products)
- Empty `packages/` placeholders (no shared TS libs yet)

**Updated:** README, memory bank, CHANGELOG, deployment docs, implementation phase paths (`backend/` + `frontend/`), `.gitignore`.

## 2026-09-28 — Consulting MVP rebuilt (Phases 1–10)

**Context:** Consulting API + web were accidentally deleted during monorepo folder move. Full MVP rebuilt from phase docs in a single session.

**Delivered:**
- `backend/consulting-api` — auth, CRM, projects, meetings, proposals, invoicing, portal, public site, AI assistant, hardening
- `frontend/consulting-web` — admin routes + `/site/[slug]` + `/portal` + `/ai`
- Flyway V1–V9, CI jobs, `render-consulting.yaml` with monorepo Docker build

**Verified:** BUILD SUCCESS — 136 consulting-api tests (135 run, 1 skipped without Docker); consulting-web production build

## 2026-09-28 — Monorepo restructure (`backend/` + `frontend/`)

**Delivered:**
- `backend/` — all Java APIs + `libs/fixna-platform-common`
- `frontend/` — all Next.js apps
- Root keeps shared docs, docker-compose, render blueprints, scripts, tools
- LocalBoost tests pass (236 total including platform-common)

## 2026-09-28 — Consulting aligned to product scaffolding

**Delivered:**
- Self-contained `backend/consulting-api/Dockerfile` + `.dockerignore`
- `render-consulting.yaml` (replaces root `render.yaml`)
- `docs/scaffolding.md` — template for LocalBoost, Consulting, future Hospitality

**Pattern:** each product = `frontend/{product}-web` + `backend/{product}-api` + `render-{product}.yaml`

## 2026-09-28 — LocalBoost migrated into fixna-ecosystem

**Delivered:**
- `backend/localboost-api` — backend from `fixna-localboost` (Spring Boot 3.5.6, V1–V9 migrations)
- `frontend/localboost-web` — frontend from `fixna-localboost/frontend`
- `render-localboost.yaml`, CI jobs, `docs/deployment/localboost.md`

**Notes:** LocalBoost and Consulting remain separate deploy units and databases. Production still on standalone repo until cutover.

## 2026-09-28 — Phase 10 complete (Consulting MVP v1.0.0)

**Delivered:**
- `application-prod.yml` + `ProductionSecurityValidator` + `ProdEnvironmentValidator`
- `Dockerfile`, `render-consulting.yaml`, enhanced CI workflow
- `docs/deployment/consulting.md`
- Hardening tests: `ProductionSecurityValidatorTest`, `DeploymentReadinessTest`, `PublicEndpointComplianceTest`

**Verified:** BUILD SUCCESS — 136 tests + consulting-web production build

## 2026-09-28 — Phase 9 complete

**Delivered:**
- `V9__ai_assistance.sql` — ai_usage_logs
- `AiProvider` + `MockAiProvider` (advisory-only)
- `/api/v1/ai/*` endpoints + `AI_USE` permission
- Frontend `/ai` + `ai-api.ts`
- Unit tests: `MockAiProviderTest`, `AiAssistantApplicationServiceTest`

## 2026-09-28 — Phase 8 complete

**Delivered:**
- `V8__public_website.sql` — profile slug/publish, testimonials
- Testimonial CRUD + public site API `/public/sites/{slug}`
- Frontend `/site/[slug]` and `/website` admin
- Unit tests: `PublicWebsiteServiceTest`, `TestimonialApplicationServiceTest`

## 2026-09-28 — Phase 7 complete

**Delivered:**
- `V7__client_portal.sql` — refresh token client scope, portal membership index
- Portal auth — JWT `clientId` claim, `/portal/auth/login`, refresh support
- `PortalApplicationService` + `/api/v1/portal/*` endpoints
- Frontend `/portal` + `portal-api.ts`
- Unit tests: `PortalApplicationServiceTest`, JWT/TenantContext updates

## 2026-09-28 — Phase 6 complete

**Delivered:**
- `V6__invoicing.sql` — invoices, invoice_lines, payments
- `billing/` — PaymentProvider, ManualPaymentProvider
- Invoice lifecycle with partial/full payment tracking
- Create from approved proposal (copies items)
- Frontend `/invoices`

## Post-MVP

Consulting MVP v1.0.0 complete. See `activeContext.md` for integration opportunities.
