# Changelog

All notable changes to the Fixna Ecosystem (Consulting track) are documented here.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Changed — Cross-product styling consistency (LocalBoost ↔ Consulting)

- Unified both web apps on a single Fixna design system: identical design tokens, base typography, focus ring, brand wordmark, public header/footer, sidebar navigation, buttons, forms, alerts, and table styles
- Removed the LocalBoost `product.css` `:root` palette override (`--primary: #17634e` etc.); both apps now use the canonical green palette (`--primary: #163e32`)
- Aligned LocalBoost with Consulting on h1/h2 type scale, base font size (1rem), body gradient background, focus offset, and sidebar/shell width (250px)
- Replaced off-palette colors in LocalBoost (blue `#eaf0fa` tables/pre, bluish status-badge neutral and auth-card shadows, hardcoded greens) with design tokens
- Matched shared component metrics across apps: buttons (46px, radius 10, weight 700, lift hover, `.button`/`.button-small`/`.button-secondary` variants), inputs (46px, radius 9, `#fbfdfb`, focus ring), boxed `[role="alert"]` banners, form cards (radius 16 + `--shadow`)
- Markup parity: LocalBoost `Brand` now uses the shared `brand-wordmark` structure and `header-cta` CTA class; sidebar sign-in/sign-out actions aligned; Consulting gained skip links and `#main-content` targets
- **Consulting fix:** `select` elements now inherit `font` (used in 8+ forms); checkbox/radio accent color added
- LocalBoost product-specific compositions (auth story, landing, dashboard panels) retained but expressed with the shared tokens

### Changed — Consulting Fixna branding refresh

- Added a reusable Fixna `f•` brand mark and shared SVG icon assets for the Consulting product
- Updated the Consulting app shell with a polished Fixna wordmark, responsive navigation, workspace labels, and branded actions
- Reworked the home page, public website, forms, cards, and interaction states to use a unified green Fixna visual system
- Added favicon, Apple icon, and web manifest metadata for browser and mobile presentation
- Updated the Consulting public site and product headers to maintain consistent branding across authenticated and public pages
- Verified the Consulting web production build and editor diagnostics after the visual refresh

### Changed — Architecture validation corrections

- **LocalBoost Docker:** monorepo build includes `fixna-platform-common`; `render-localboost.yaml` uses `dockerContext: .`
- **Maven bootstrap:** `npm run bootstrap:maven` + `scripts/bootstrap-maven.{sh,cmd}` install parent POM and platform-common for independent product builds
- **Consulting local DB:** default `POSTGRES_USER` aligned to `postgres` (docker-compose)
- **Docs:** structured `docs/architecture/`, `docs/products/`, ADR-007/ADR-008
- **CI:** installs parent POM before product API jobs
- **Hospitality:** removed placeholder `package.json` from `frontend/hospitality-web`

### Removed — Unused workspace placeholders

- Deleted empty `packages/` folder (README-only stubs, no `package.json`)
- npm workspaces now cover `frontend/*` only; root `node_modules` remains for hoisted deps

### Removed — Stale artifacts

- Deleted leftover `services/` folder (lock files from failed restructure move)
- Removed superseded `RESTORE-CONSULTING.md`, `REBUILD-CONSULTING.md`
- Removed redundant `docker-compose.localboost.yml` (use root `docker-compose.yml`)

### Added — Consulting MVP rebuilt after accidental deletion

- Full rebuild of `backend/consulting-api` + `frontend/consulting-web` from `docs/implementation/phase-*.md`
- Phases 1–10: auth/RBAC, CRM, projects, meetings, proposals, invoicing, portal, public site, AI assistant, hardening
- **136 API tests**; CI consulting jobs; monorepo Docker build in `render-consulting.yaml`

### Changed — Monorepo layout (`backend/` + `frontend/`)

- Renamed `services/` → `backend/`, `apps/` → `frontend/`
- Shared library at `backend/libs/fixna-platform-common`
- Updated CI, render blueprints, docs, and scripts to new paths

### Changed — Shared platform library (`backend/libs/fixna-platform-common`)

- Extracted duplicated logging, security, CORS, and web error handling from both API services
- Maven parent `pom.xml` reactor: common → consulting-api → localboost-api
- `TenantScopeProvider` bridge per product for tenant-aware MDC logging

### Added — Full monorepo readiness (Consulting + LocalBoost)

- **Database docs:** `docs/database/README.md` + per-service `db/README.md` (migration index V1–V9)
- **Docker Compose:** root `docker-compose.yml` (Consulting :5433 + LocalBoost :5432 + Redis)
- **LocalBoost legacy content migrated:** `docs/products/localboost/`, `ai/localboost/`, `.cursor/products/localboost/`, `tools/localboost/`, `scripts/localboost/`, `infrastructure/localboost/`
- **`AGENTS.md`** + `.cursor/README.md` — agent rules and workflows
- **`vercel.json`** for both web apps (monorepo install)
- **LocalBoost:** `DeploymentReadinessTest`, prod swagger lockdown, `render-localboost.yaml` production parity

### Changed — Unified product scaffolding (Consulting aligned with LocalBoost)

- **`backend/consulting-api/Dockerfile`** — monorepo build including platform-common
- **`render-consulting.yaml`** — replaces root `render.yaml`; `dockerContext: .` + `buildFilter`
- **`docs/scaffolding.md`** — reusable template for adding products
- Root README, deployment docs, and service READMEs updated for per-product deploy pattern

### Added — LocalBoost migration into ecosystem monorepo

- **`backend/localboost-api`** — full backend copied from `fixna-localboost` (187 Java sources, Flyway V1–V9, tests)
- **`frontend/localboost-web`** — Next.js app copied from `fixna-localboost/frontend`
- **`render-localboost.yaml`** — Render blueprint with monorepo paths
- **CI** — LocalBoost API test/package + web build jobs
- **`docs/deployment/localboost.md`** — deployment and isolation notes

Consulting MVP unchanged; LocalBoost keeps its own database and migration history.

## [1.0.0] — 2026-09-28

### Added — Phase 10: Hardening & Deployment (MVP complete)

**Backend (`backend/consulting-api`)**
- `application-prod.yml` — HSTS, swagger disabled, hardened actuator/errors/logging
- `ProductionSecurityValidator` — fail-fast on weak JWT secret or localhost CORS in prod
- `Permissions-Policy` security header
- `Dockerfile` multi-stage JRE 21 build with readiness healthcheck
- Unit tests: `ProductionSecurityValidatorTest`, `DeploymentReadinessTest`, `PublicEndpointComplianceTest`

**DevOps**
- `render.yaml` — API + PostgreSQL blueprint
- `.github/workflows/ci.yml` — API test/package + consulting-web build gates
- `docs/deployment/consulting.md` — production checklist

## [0.9.0] — 2026-09-28

### Added — Phase 9: AI Assistance

**Backend (`backend/consulting-api`)**
- Flyway migration `V9__ai_assistance.sql` — `ai_usage_logs`
- `AiProvider` + `MockAiProvider` advisory suggestion engine
- `AI_USE` permission for consultants
- REST API `/api/v1/ai/*` — proposal, meeting agenda, client summary, website copy
- Usage logging + audit events for every suggestion
- Unit tests: `MockAiProviderTest`, `AiAssistantApplicationServiceTest`

**Frontend (`frontend/consulting-web`)**
- `/ai` assistant page
- `src/lib/ai-api.ts`

## [0.8.0] — 2026-09-28

### Added — Phase 8: Public Website

**Backend (`backend/consulting-api`)**
- Flyway migration `V8__public_website.sql` — profile slug/publish, testimonials
- `Testimonial` CRUD at `/api/v1/testimonials`
- Extended profile with `publicSlug` and `published`
- Public read API `GET /api/v1/public/sites/{slug}` (unauthenticated)
- Unit tests: `PublicWebsiteServiceTest`, `TestimonialApplicationServiceTest`

**Frontend (`frontend/consulting-web`)**
- `/site/[slug]` public marketing page
- `/website` admin page for profile, publish settings, testimonials
- `src/lib/public-api.ts`, `src/lib/website-api.ts`

## [0.7.0] — 2026-09-28

### Added — Phase 7: Client Portal

**Backend (`backend/consulting-api`)**
- Flyway migration `V7__client_portal.sql` — portal refresh token client scope
- JWT `clientId` claim for portal sessions
- `POST /api/v1/portal/auth/login` — client portal authentication
- Portal refresh via shared `/api/v1/auth/refresh` when refresh token has `client_id`
- `PortalApplicationService` + `/api/v1/portal/*` — client-scoped projects, proposals, invoices, meetings
- Proposal approve/reject from portal (CLIENT_ADMIN)
- Unit tests: `PortalApplicationServiceTest`, updated JWT/TenantContext tests

**Frontend (`frontend/consulting-web`)**
- `/portal` page — portal login and tabbed client views
- `src/lib/portal-api.ts`

## [0.6.0] — 2026-09-28

### Added — Phase 6: Invoicing

**Backend (`backend/consulting-api`)**
- Invoice, InvoiceLine, Payment with send/pay lifecycle
- `PaymentProvider` + `ManualPaymentProvider` in `billing/`
- Create invoice from approved proposal (copies line items)
- Flyway migration `V6__invoicing.sql`
- REST API `/api/v1/invoices` including `/send` and `/payments`
- Domain events: `InvoiceCreated`, `InvoiceSent`, `PaymentRecorded`, `InvoicePaid`
- Unit tests: `InvoiceApplicationServiceTest`, `ManualPaymentProviderTest`, `InvoiceLifecycleTest`

**Frontend (`frontend/consulting-web`)**
- `/invoices` page — draft, line items, send, record payments
- `src/lib/invoices-api.ts`

## [0.5.0] — 2026-09-28

### Added — Phase 5: Proposals

**Backend (`backend/consulting-api`)**
- Proposal and ProposalItem with line-item totals and send/approve/reject/cancel lifecycle
- Flyway migration `V5__proposals.sql`
- REST API `/api/v1/proposals` including `/send`, `/approve`, `/reject`
- Domain events: `ProposalCreated`, `ProposalSent`, `ProposalApproved`, `ProposalRejected`
- Unit tests: `ProposalApplicationServiceTest`, `ProposalLifecycleTest`

**Frontend (`frontend/consulting-web`)**
- `/proposals` page — draft, line items, send, approve/reject
- `src/lib/proposals-api.ts`

## [0.4.0] — 2026-09-28

### Added — Phase 4: Meetings & Calendar

**Backend (`backend/consulting-api`)**
- Meeting entity with client/engagement/project links and lifecycle transitions
- `CalendarProvider` abstraction with `InternalCalendarProvider` (conflict detection, time validation)
- Flyway migration `V4__meetings.sql`
- REST API `/api/v1/meetings` with transition endpoint
- Domain events: `MeetingScheduled`, `MeetingStatusChanged`
- Unit tests: `MeetingApplicationServiceTest`, `InternalCalendarProviderTest`, `MeetingLifecycleTest`

**Frontend (`frontend/consulting-web`)**
- `/meetings` page — schedule, start, complete, cancel meetings
- `src/lib/meetings-api.ts` API client
- Home page link to Meetings

## [0.3.0] — 2026-09-28

### Added — Phase 3: Project Management

**Backend (`backend/consulting-api`)**
- Engagement, Project, Milestone, Task domain models with validated lifecycle transitions
- Flyway migration `V3__project_management.sql`
- Packages: `consulting/engagement/`, `consulting/project/`
- Domain events: `EngagementCreated`, `EngagementStatusChanged`, `ProjectCreated`, `ProjectStatusChanged`
- REST API for engagements, projects, milestones, and tasks (including `/transition` endpoints)
- Unit tests: `EngagementApplicationServiceTest`, `ProjectApplicationServiceTest`, `ProjectLifecycleTest`

**Frontend (`frontend/consulting-web`)**
- `/projects` page — create engagements, projects, tasks; trigger status transitions
- `src/lib/projects-api.ts` API client
- Home page link to Projects

**Tests:** 35 total (34 run, 1 skipped — Docker/Testcontainers context load)

## [0.2.0] — 2026-09-28

### Added — Phase 2: Consulting CRM

- Client, ClientContact, ClientPortalMembership
- Consulting Profile, Service catalog
- Flyway migration `V2__consulting_crm.sql`
- Domain events: `ClientCreated`, `ClientUpdated`
- Frontend `/clients` route

## [0.1.0] — 2026-09-28

### Added — Phase 1: Platform Foundation

- Identity, JWT auth, tenancy, membership, permission-based RBAC
- Audit, notification/file abstractions, domain events, observability
- Flyway migration `V1__platform_core.sql`
- Health endpoints, error envelope, tenant isolation patterns
