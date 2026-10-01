# Repository Assessment — Fixna Ecosystem

> **Historical document (2026-09-28 bootstrap).** Written before Consulting MVP and monorepo migration. Current layout uses `backend/` + `frontend/`; see [activeContext.md](../memory-bank/activeContext.md).

**Task:** TASK-001 — Repository Assessment  
**Date:** 2026-09-28  
**Assessed repository:** `fixna-ecosystem` (`c:\Users\Dell\workspace\fixna-ecosystem`)  
**Related live product:** `fixna-localboost` (`c:\Users\Dell\workspace\fixna-localboost`)

---

## Executive Summary

`fixna-ecosystem` is a **structural scaffold only**. It defines the intended multi-product monorepo layout (apps, services, shared packages) but contains **no application source code**, **no database**, **no tests**, **no CI**, and **no git history**.

The only fully implemented Fixna product today lives in the sibling repository **`fixna-localboost`**, deployed at `app.fixna.in` / `api.fixna.in`. That codebase already implements many platform primitives (identity, tenancy, JWT auth, audit, observability, adapter patterns) under the `in.fixna.platform` Java package, alongside LocalBoost-specific domain modules.

**Conclusion:** Phase 1 (Platform Foundation) and all subsequent phases must be built from scratch inside `fixna-ecosystem`, **reusing conventions and patterns from LocalBoost** rather than migrating or rewriting LocalBoost itself.

---

## 1. Repository Structure

### 1.1 `fixna-ecosystem` (this repository)

```
fixna-ecosystem/
├── apps/
│   ├── localboost-web/          # placeholder — package.json + README only
│   ├── consulting-web/          # placeholder — package.json + README only
│   └── hospitality-web/         # placeholder — future product
├── services/
│   ├── localboost-api/          # placeholder — README only
│   ├── consulting-api/            # placeholder — README only
│   └── hospitality-api/           # placeholder — README only
├── packages/
│   ├── api-client/              # placeholder
│   ├── auth/                    # placeholder
│   ├── billing-ui/              # placeholder
│   ├── charts/                  # placeholder
│   ├── forms/                   # placeholder
│   ├── notifications/           # placeholder
│   ├── scheduling/              # placeholder
│   ├── tables/                  # placeholder
│   ├── types/                   # placeholder
│   └── ui/                      # placeholder
├── docs/
│   ├── architecture.md
│   ├── build-and-deployment.md
│   └── implementation/
│       └── repository-assessment.md   # this document
├── package.json                 # root build scripts only
└── README.md
```

**Observations:**

| Aspect | Status |
|--------|--------|
| Git repository | **Not initialized** — no `.git`, no remotes, no CI |
| Workspaces / monorepo tooling | **Absent** — no `pnpm-workspace.yaml`, no npm workspaces, no Turborepo |
| Backend source | **Absent** — no `pom.xml`, no Java files |
| Frontend source | **Absent** — no `src/`, no `next.config`, no `tsconfig` |
| Shared packages | **Named only** — README placeholders, no `package.json` in packages |
| ADRs | **Absent** — required ADR-001 through ADR-006 not yet created |
| `.env.example` | **Absent** |
| Docker / infra | **Absent** |

The scaffold correctly encodes the **product isolation principle**: each app and service is an independent deployable unit with its own database. This aligns with the master implementation prompt and `docs/architecture.md`.

### 1.2 `fixna-localboost` (live product, sibling repo)

```
fixna-localboost/
├── backend/                     # Java 21 + Spring Boot 3.5.6
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/in/fixna/platform/
├── frontend/                    # Next.js 16 + React 19 + TypeScript
│   ├── package.json
│   ├── vercel.json
│   └── src/
├── docs/                        # extensive product/ops documentation
├── infrastructure/demo/         # Render/Vercel env templates
├── tools/                       # local demo launcher, SQL seeds
├── render.yaml
├── .github/workflows/ci.yml
└── .env.example
```

LocalBoost is a **single-product monolith** (one backend, one frontend) — not yet split into the ecosystem layout. Its backend package name (`in.fixna.platform`) already reflects platform-oriented naming, with LocalBoost domains layered on top.

---

## 2. Backend Stack

### 2.1 `fixna-ecosystem`

**No backend exists.** `backend/consulting-api/` contains only a README referencing the Controller → Application Service → Domain → Repository convention.

### 2.2 `fixna-localboost` (reference implementation)

| Item | Value |
|------|-------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Build | Maven (`backend/pom.xml`) |
| Artifact | `in.fixna:fixna-api:1.0.0` → `fixna-api.jar` |
| ORM | Spring Data JPA (`ddl-auto: validate`) |
| Database driver | PostgreSQL |
| Migrations | Flyway 11.20.0 |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Validation | Jakarta Bean Validation |
| Logging | Log4j2 (Logback excluded) |
| Cache | Redis (local/dev; disabled on staging) |
| Observability | Micrometer + OpenTelemetry OTLP bridge, Spring Actuator |
| API docs | springdoc-openapi 2.8.13 |
| Testing | JUnit 5, Mockito, Testcontainers (PostgreSQL) |
| Utilities | Lombok |

**Recommended for Consulting API:** Match this stack exactly to preserve team familiarity and enable future platform extraction.

---

## 3. Frontend Stack

### 3.1 `fixna-ecosystem`

Each app (`frontend/*-web`) has a minimal `package.json`:

```json
{
  "dependencies": {
    "next": "latest",
    "react": "latest",
    "react-dom": "latest"
  }
}
```

**Missing:** TypeScript, ESLint, Tailwind/CSS framework, test runner, shared package references, `src/` directory, `next.config`, lock files.

Using `"latest"` for dependencies is a **risk** — builds are not reproducible.

### 3.2 `fixna-localboost` (reference implementation)

| Item | Value |
|------|-------|
| Framework | Next.js 16.3.6 (App Router) |
| UI | React 19.1 |
| Language | TypeScript 5 |
| HTTP client | Axios 1.7 |
| Data fetching | TanStack React Query 5 |
| Forms | React Hook Form 7 + Zod 4 |
| Testing | Vitest 3 |
| Structure | `frontend/src/app/` (routes), `frontend/src/lib/` (API clients), `frontend/src/components/` |

**Recommended for Consulting Web:** Next.js + React + TypeScript, with shared UI extracted into `packages/ui` over time. Pin exact dependency versions (not `"latest"`).

---

## 4. Database

### 4.1 `fixna-ecosystem`

**No database configuration exists.** No connection strings, no schema, no Flyway setup.

### 4.2 `fixna-localboost` (reference)

| Item | Value |
|------|-------|
| Engine | PostgreSQL |
| Local DB name | `localboost` |
| Production | Neon PostgreSQL (external, not Render-managed) |
| Tenancy model | Shared schema, `tenant_id` on all tenant-owned tables |
| Timestamps | UTC (application convention) |
| RLS | Designed for future Row-Level Security; currently application-layer only |

**Consulting requirement:** Isolated Consulting database (`consulting` or Neon project), separate from LocalBoost. Never share tables or merge schemas.

---

## 5. Migrations

### 5.1 `fixna-ecosystem`

**No migrations.**

### 5.2 `fixna-localboost` (reference)

Location: `backend/src/main/resources/db/migration/`

| Version | File | Scope |
|---------|------|-------|
| V1 | `create_tenants_users.sql` | `tenants`, `users`, `tenant_memberships` |
| V2 | `create_businesses.sql` | `businesses`, `business_locations` |
| V3 | `create_campaigns.sql` | `campaigns`, `campaign_offers` |
| V4 | `create_targeting_creatives.sql` | `audiences`, `geo_targets`, `creatives`, `campaign_channels` |
| V5 | `create_platform_analytics_leads.sql` | `platform_connections`, `campaign_metrics`, `leads` |
| V6 | `create_ai_billing_audit.sql` | `ai_recommendations`, `ai_usage`, `subscriptions`, `audit_logs` |
| V7 | `create_refresh_tokens.sql` | `refresh_tokens` |
| V8 | `create_ai_usage_log.sql` | `ai_usage_log` |
| V9 | `plans_and_ai_quota.sql` | Performance indexes |

**Convention:** `V{n}__{description}.sql`, Flyway-managed, `ddl-auto: validate`.

**For Consulting:** Start fresh with `V1__platform_core.sql` (identity, tenant, membership, RBAC, audit) and subsequent consulting-domain migrations. Do not copy LocalBoost campaign/business tables.

---

## 6. Authentication

### 6.1 `fixna-ecosystem`

**Not implemented.**

### 6.2 `fixna-localboost` (reference)

| Component | Class / Path |
|-----------|-------------|
| JWT service | `auth/JwtService` |
| JWT config | `auth/JwtProperties` (`fixna.security.jwt.*`) |
| Auth filter | `auth/JwtAuthenticationFilter` |
| Auth service | `auth/AuthService` |
| Auth controller | `auth/AuthController` → `/api/v1/auth/*` |
| Refresh tokens | `auth/RefreshToken` (hashed, single-use rotation) |
| Password hashing | BCrypt via `common/util/PasswordUtility` |

**Flow:**
1. Register creates `User` + `Tenant` + membership atomically.
2. Login resolves tenant **server-side** from memberships.
3. Access token (15 min) + refresh token (7 days) issued.
4. JWT claims: `sub` (userId), `tenantId`, `role`, `typ`.
5. `MembershipLookup` re-validates membership on every request.
6. Logout revokes all refresh tokens.

**Frontend:** Refresh token stored in `localStorage` (`fixna.refreshToken`). Access token managed via `auth-context.tsx`.

**Consulting considerations:**
- Reuse JWT contract shape for consistency across Fixna products.
- Consulting adds `CLIENT_ADMIN` / `CLIENT_USER` roles and `ClientPortalMembership` — these are new concepts not present in LocalBoost.
- Do not change LocalBoost auth endpoints or JWT claim structure.

---

## 7. Tenancy

### 7.1 `fixna-ecosystem`

**Not implemented.**

### 7.2 `fixna-localboost` (reference)

| Component | Purpose |
|-----------|---------|
| `common/tenant/TenantContext` | ThreadLocal: `tenantId`, `userId`, `MembershipRole` |
| `common/tenant/AuthenticatedUser` | Spring Security principal |
| `tenant/Tenant` | Entity with `TenantType` (SMB, AGENCY, ENTERPRISE, INTERNAL) |
| `tenant/TenantMembership` | User ↔ Tenant link with role |
| `tenant/TenantService` | Tenant administration |

**Enforcement pattern:**
- `TenantContext` set by `JwtAuthenticationFilter` from JWT — **never from request body/params**.
- Services call `TenantContext.requireTenantId()`.
- Repositories use `findByIdAndTenantId(id, tenantId)`.
- Cross-tenant access returns **404** (not 403) to prevent IDOR leakage.
- Automated sweeps: `CrossTenantSweepTest`, `RepositoryTenantScopeSweepTest`.
- Static guard: `NonNegotiablesComplianceTest` ensures DTOs exclude `tenantId`.

**This is the pattern Consulting must adopt.** It is mature and test-backed.

---

## 8. RBAC

### 8.1 `fixna-ecosystem`

**Not implemented.**

### 8.2 `fixna-localboost` (reference)

**Roles** (`MembershipRole` enum):
- `TENANT_OWNER`, `TENANT_ADMIN`, `TENANT_MARKETING_MANAGER`, `TENANT_MARKETING_USER`, `TENANT_VIEWER`
- `AGENCY_ADMIN`, `AGENCY_USER`

**Authorization approach:**
- Programmatic via `TenantContext` methods (`canWrite()`, `canAdminister()`, `requireWrite()`, `requireAdmin()`).
- Spring Security grants `ROLE_<MembershipRole>` but no `@PreAuthorize` usage.
- **No permission tables** — capabilities are enum methods only.
- Platform admin: `AdminService.requirePlatformAdmin()` gates on `TenantType.INTERNAL`.

**Gap vs. master prompt requirements:**

The master prompt specifies a **permission-based** RBAC model with granular permissions (`CLIENT_VIEW`, `PROJECT_MANAGE`, `PROPOSAL_APPROVE`, etc.) and roles like `CONSULTANT_ADMIN`, `CONSULTANT`, `CLIENT_ADMIN`, `CLIENT_USER`.

LocalBoost uses a **simpler role-enum model** without a permission matrix. Consulting will need to **extend or replace** this pattern — either:
1. Introduce a `Permission` enum + role-permission mapping (recommended per master prompt), or
2. Extend `MembershipRole` with Consulting-specific roles and enum methods.

**Do not retrofit LocalBoost's role model** — Consulting RBAC should be designed in `backend/consulting-api` independently.

---

## 9. Logging

### 9.1 `fixna-ecosystem`

**Not implemented.**

### 9.2 `fixna-localboost` (reference)

| Item | Detail |
|------|--------|
| Framework | Log4j2 (`log4j2-spring.xml`) |
| API | SLF4J |
| Local/test | Human-readable pattern with MDC |
| dev/staging/prod | JSON layout (ELK/Datadog-ready) |
| Named loggers | `fixna.access`, `fixna.audit`, `fixna.telemetry`, `fixna.error`, `fixna.ratelimit`, `fixna.notify` |
| MDC fields | `traceId`, `spanId`, `requestId`, `tenantId`, `userId`, `campaignId`, `operation` |
| Masking | `common/logging/SensitiveDataMasker` |
| Context setup | `common/logging/LoggingContext` |

**No `System.out`, `printStackTrace`, or password/token logging** — enforced by convention and compliance tests.

---

## 10. Observability

### 10.1 `fixna-ecosystem`

**Not implemented.**

### 10.2 `fixna-localboost` (reference)

| Capability | Implementation |
|------------|---------------|
| Health (public) | `GET /api/v1/health` — aggregated status, version, components |
| Readiness | `GET /api/v1/health/ready` |
| Actuator | `/actuator/health`, `/actuator/info` (public); `/actuator/prometheus` (restricted) |
| Tracing | Micrometer Tracing + OTel OTLP bridge |
| Request correlation | `X-Request-Id` via `RequestIdFilter` |
| Operation timing | `common/observability/OperationTimer` |
| Build metadata | Maven `build-info` goal → version in health response |
| Deploy metadata | `FIXNA_DEPLOYED_AT`, `FIXNA_APP_ENV` env vars |

**Consulting should replicate** the health endpoint contract and MDC field set (adding `clientId`, `projectId` as needed per master prompt).

---

## 11. Reusable Components

### 11.1 `fixna-ecosystem` packages (planned, empty)

| Package | Intended purpose | Status |
|---------|-----------------|--------|
| `@fixna/ui` | Button, Input, Modal, DataTable, StatusBadge, etc. | Placeholder |
| `@fixna/api-client` | Typed HTTP client, error schema | Placeholder |
| `@fixna/auth` | Auth context, token management | Placeholder |
| `@fixna/forms` | Form primitives with validation | Placeholder |
| `@fixna/tables` | DataTable, Pagination, FilterBar | Placeholder |
| `@fixna/charts` | MetricCard, charts | Placeholder |
| `@fixna/notifications` | In-app notification UI | Placeholder |
| `@fixna/scheduling` | Calendar, DateTimePicker | Placeholder |
| `@fixna/billing-ui` | CurrencyInput, invoice display | Placeholder |
| `@fixna/types` | Shared TypeScript types | Placeholder |

None have `package.json`, source files, or build configuration.

### 11.2 `fixna-localboost` (extractable patterns)

**Backend (Java) — candidates for future platform library:**

| Pattern | Location | Reusability |
|---------|----------|-------------|
| TenantContext + JWT filter | `common/tenant/`, `auth/` | High |
| ApiError / GlobalExceptionHandler | `common/web/` | High |
| RequestIdFilter, RateLimitFilter | `common/web/` | High |
| LoggingContext + SensitiveDataMasker | `common/logging/` | High |
| AuditPublisher | `common/audit/` | High |
| PlatformHealthService | `common/observability/` | High |
| Adapter registry pattern | `platform/PlatformAdapterRegistry` | High |
| Plan/quota checker pattern | `billing/PlanLimitChecker` | Medium (Consulting uses invoice billing, not subscriptions) |
| Compliance test suite | `NonNegotiablesComplianceTest` | High |

**Frontend (TypeScript) — candidates for `packages/`:**

| Component / Module | Location | Reusability |
|-----------------|----------|-------------|
| `api-client.ts` | `frontend/src/lib/` | High |
| `auth-context.tsx` | `frontend/src/lib/` | High |
| `ui.tsx` (Button, Input, etc.) | `frontend/src/components/` | High |
| `auth-layout.tsx`, `app-shell.tsx` | `frontend/src/components/` | Medium |
| Zod API error schema | `frontend/src/lib/api-schemas.test.ts` | High |
| Brand components | `frontend/src/brand/` | Low (product-specific) |

**Strategy:** Do not extract from LocalBoost until Consulting has its own working copy of needed primitives. Extract to `packages/` only when duplication is proven.

---

## 12. Adapters

### 12.1 `fixna-ecosystem`

**Not implemented.**

### 12.2 `fixna-localboost` (reference adapter patterns)

| Adapter type | Interface | Implementations |
|-------------|-----------|-----------------|
| Ad platform | `PlatformAdapter` | Google, Meta, WhatsApp (all mock on demo) |
| AI provider | `AIProvider` | Mock (deterministic), extensible to OpenAI |
| Notification | `NotificationProvider` | In-process mock |
| Registry | `PlatformAdapterRegistry` | Plugin discovery pattern |

**Consulting adapters needed (per master prompt):**

| Adapter | Initial impl | Future |
|---------|-------------|--------|
| `PaymentProvider` | `ManualPaymentProvider` | Razorpay, Stripe |
| `CalendarProvider` | `InternalCalendarProvider` | Google, Microsoft |
| `FileStorage` | `LocalFileStorage` | S3-compatible |
| `AIProvider` | `MockAIProvider` | OpenAI |
| Notification channels | IN_APP, EMAIL | WhatsApp, SMS, PUSH |

LocalBoost adapters (ad platforms) must **not** be reused or modified for Consulting.

---

## 13. API Conventions

### 13.1 `fixna-ecosystem`

**Not implemented.** README references Controller → Application Service → Domain → Repository layering.

### 13.2 `fixna-localboost` (reference)

| Convention | Detail |
|------------|--------|
| Base path | `/api/v1/` |
| Error envelope | `{ timestamp, status, code, message, path, requestId }` |
| Error codes | Stable string codes via `FixnaException` (e.g. `VALIDATION_FAILED`, `NOT_FOUND`) |
| Validation | Jakarta `@Valid` on request DTOs/records |
| Tenant safety | No `tenantId` in client-facing DTOs |
| Request ID | `X-Request-Id` header propagated and echoed |
| OpenAPI | `/v3/api-docs`, `/swagger-ui.html` |
| IDOR prevention | Cross-tenant → 404, not 403 |

**Consulting API examples (from master prompt):**

```
GET  /api/v1/clients
POST /api/v1/clients
GET  /api/v1/projects
POST /api/v1/proposals/{id}/send
POST /api/v1/proposals/{id}/approve
POST /api/v1/invoices/{id}/send
```

Consulting should adopt the same error envelope and `/api/v1` prefix for consistency.

---

## 14. Testing Conventions

### 14.1 `fixna-ecosystem`

**No tests exist.**

### 14.2 `fixna-localboost` (reference)

**Backend** (`backend/src/test/java/`, 54 test files):

| Pattern | Purpose | Examples |
|---------|---------|---------|
| Unit tests | Service/domain logic | `CampaignServiceTest`, `AuthServiceTest` |
| Tenant isolation sweeps | Cross-tenant safety | `CrossTenantSweepTest`, `RepositoryTenantScopeSweepTest` |
| Static compliance | Architecture guardrails | `NonNegotiablesComplianceTest` |
| Integration (Testcontainers) | Full journey with real PG | `FixnaEndToEndTest` |
| Filter/web layer | HTTP concerns | `RequestIdFilterTraceTest`, `RateLimitFilterTest` |
| Config validation | Profile safety | `ProdEnvironmentValidatorTest` |

**Frontend** (Vitest):
- `display.test.ts`, `api-schemas.test.ts`

**Commands:**
- Backend: `mvn -f backend/pom.xml test`
- Frontend: `cd frontend && npm run build` (build-as-test; Vitest for unit)

**CI:** `.github/workflows/ci.yml` — Java 21 tests + Next.js build on push/PR.

**Consulting must replicate:**
- Tenant isolation sweep tests (mandatory per master prompt).
- `NonNegotiablesComplianceTest` equivalent.
- State transition tests for project/proposal/invoice lifecycles.

---

## 15. Deployment Configuration

### 15.1 `fixna-ecosystem`

**No deployment configuration.** `docs/build-and-deployment.md` documents the intended model:

| Product | Frontend | API | Database |
|---------|----------|-----|----------|
| LocalBoost | `app.fixna.in` | `api.fixna.in` | Isolated LocalBoost DB |
| Consulting | `consulting.fixna.in` | `consulting-api.fixna.in` | Isolated Consulting DB |
| Hospitality | `hospitality.fixna.in` | `hospitality-api.fixna.in` | Isolated Hospitality DB |

### 15.2 `fixna-localboost` (reference)

| File | Purpose |
|------|---------|
| `render.yaml` | Render Blueprint — Docker API, free tier, health check `/actuator/health` |
| `backend/Dockerfile` | Multi-stage Maven 21 → JRE 21 |
| `frontend/vercel.json` | Vercel Next.js deploy |
| `.env.example` | DB, Redis, JWT, AI, OTEL vars |
| `frontend/.env.example` | `NEXT_PUBLIC_API_BASE_URL`, `NEXT_PUBLIC_APP_ENV` |
| `infrastructure/demo/render.env.example` | Render env template |
| `infrastructure/demo/vercel.env.example` | Vercel env template |

**Profiles:** `application.yml`, `local`, `dev`, `staging`, `prod`, `test`.

**Consulting deployment target:** Same stack (Render API + Vercel frontend + Neon PostgreSQL), different subdomains and env vars. No shared Render service with LocalBoost.

---

## 16. Gaps

### Critical gaps (must address before Phase 1)

| # | Gap | Impact |
|---|-----|--------|
| G1 | No git repository | No version control, no CI, no collaboration |
| G2 | No backend source code in ecosystem | All platform + consulting code must be created |
| G3 | No frontend source code in ecosystem | All UI must be created |
| G4 | No monorepo tooling (workspaces, Turborepo) | Cannot build/share packages across apps |
| G5 | No `package.json` in shared packages | Packages cannot be consumed |
| G6 | No database or migrations | Schema must be designed and created |
| G7 | No `.env.example` | Developer onboarding blocked |
| G8 | No CI/CD pipeline | No automated quality gates |
| G9 | No ADRs (ADR-001 through ADR-006) | Architectural decisions undocumented |
| G10 | `"latest"` dependency versions | Non-reproducible builds |
| G11 | No permission-based RBAC (only role-enum in LocalBoost) | Consulting needs new RBAC design |
| G12 | No Consulting-specific domain model | All entities must be created |
| G13 | No event/outbox infrastructure | Domain events are in-process only in LocalBoost |
| G14 | No file storage abstraction | Must be built for Consulting documents |
| G15 | No payment provider abstraction | Must be built for Consulting invoices |
| G16 | No calendar/scheduling abstraction | Must be built for Consulting meetings |
| G17 | LocalBoost not integrated into ecosystem layout | Lives in separate repo with different structure |
| G18 | No client portal auth model | `ClientPortalMembership` is a new concept |
| G19 | No public website routes | Marketing pages must be built |
| G20 | No lock files (`package-lock.json`) | Non-reproducible npm installs |

### Moderate gaps

| # | Gap | Notes |
|---|-----|-------|
| G21 | Redis configured in LocalBoost but disabled on staging | Decide if Consulting needs Redis |
| G22 | `ai_recommendations` table in LocalBoost has no JPA entity | Not relevant to Consulting |
| G23 | No TypeScript in ecosystem apps | Must be added |
| G24 | No test framework in ecosystem | Must be added |
| G25 | No Docker setup in ecosystem | Needed for API deployment |

---

## 17. Conflicting Conventions

| Area | Ecosystem scaffold | LocalBoost (live) | Master prompt | Resolution |
|------|-------------------|-------------------|---------------|------------|
| **Repo layout** | `apps/` + `services/` + `packages/` | `frontend/` + `backend/` at root | Modular monolith in `backend/src/main/java/in/fixna/platform/` | Build Consulting in ecosystem layout; do not restructure LocalBoost |
| **Backend package** | `backend/consulting-api/` (empty) | `backend/src/main/java/in/fixna/platform/` | `common/` + `consulting/` subpackages | Use master prompt package structure inside `consulting-api` |
| **RBAC model** | Not defined | Role enum only | Permission-based with 6 roles | Design new RBAC for Consulting; don't change LocalBoost |
| **Role names** | CONSULTANT_ADMIN, CLIENT_USER, etc. | TENANT_OWNER, TENANT_ADMIN, etc. | Different role sets per product | Expected — each product owns its roles |
| **Billing model** | Not defined | Subscription + Plan (SaaS) | Invoice + Payment (consulting) | Separate billing domains; shared primitives only (Money, Currency) |
| **Dependency versions** | `"latest"` | Pinned (Next 16.3.6, etc.) | Pin versions | Pin all versions in ecosystem |
| **Frontend structure** | `frontend/consulting-web/src/` (missing) | `frontend/src/app/` + `lib/` + `components/` | `app/` + `components/` + `lib/` + `features/` | Follow master prompt; add `features/` directory |
| **Shared packages** | 10 named placeholders | All code in single frontend | Extract to `packages/` | Build packages incrementally; start inline, extract later |
| **User = Client** | Explicitly separated in prompt | N/A (no client concept) | User ≠ Client | New `Client` + `ClientPortalMembership` model |
| **Auth package** | `@fixna/auth` placeholder | `frontend/src/lib/auth-context.tsx` | Shared auth package | Extract after Consulting auth works |
| **Multi-product apps** | 3 app placeholders | 1 app | 1 app per product | Correct — each product is independent |

---

## 18. Risks

| # | Risk | Severity | Mitigation |
|---|------|----------|------------|
| R1 | **Accidental LocalBoost modification** | Critical | Never touch `fixna-localboost` during Consulting build; separate repos, separate DBs, separate deployments |
| R2 | **Premature platform extraction** | High | Build Consulting first with inline code; extract to `packages/` only when duplication is proven across 2+ products |
| R3 | **Generic domain modeling** | High | Enforce domain boundary rule — no GenericCustomer, GenericOrder, etc. |
| R4 | **Tenant ID from browser** | Critical | Replicate LocalBoost's server-side-only tenant resolution and compliance tests |
| R5 | **Shared database** | Critical | Consulting gets its own Neon project / PostgreSQL instance |
| R6 | **Breaking LocalBoost API contract** | Critical | No changes to `api.fixna.in` endpoints, JWT shape, or auth flow |
| R7 | **Over-engineering RBAC** | Medium | Start with role-permission mapping table; avoid complex policy engines in MVP |
| R8 | **Microservices temptation** | Medium | Modular monolith only; no service extraction until demonstrated need |
| R9 | **Non-reproducible builds** | Medium | Pin all dependency versions; add lock files; set up CI immediately |
| R10 | **Missing tenant isolation tests** | High | Port `CrossTenantSweepTest` pattern from LocalBoost on day one |
| R11 | **AI autonomy risk** | Medium | Enforce advisory-only AI from the start; no autonomous payments/messages |
| R12 | **Invented marketing content** | Low | Use configurable placeholders for public website; no fake testimonials/case studies |
| R13 | **No git history** | Medium | Initialize git and CI before any implementation work |
| R14 | **Ecosystem/LocalBoost code drift** | Medium | Document shared conventions; consider extracting platform JAR in Phase 10+ only |

---

## 19. Recommended Implementation Sequence

Based on the assessment, the following sequence is recommended. This aligns with the master prompt phases but accounts for the greenfield state of `fixna-ecosystem`.

### Pre-Phase: Repository Bootstrap

1. Initialize git repository.
2. Add `.gitignore`, `.env.example`.
3. Set up npm workspaces or pnpm workspaces for `apps/*` and `packages/*`.
4. Pin dependency versions in all `package.json` files.
5. Create required ADRs (ADR-001 through ADR-006).
6. Add basic CI workflow (lint + build gates).

### Phase 1: Platform Foundation (`backend/consulting-api`)

Build inside `backend/consulting-api` using the master prompt package structure:

```
backend/src/main/java/in/fixna/platform/
├── common/          # error handling, web filters, logging
├── identity/        # User entity
├── tenancy/         # Tenant, TenantContext
├── membership/      # Membership entity
├── rbac/            # Permission enum, role-permission mapping
├── audit/           # AuditPublisher, AuditLog
├── notification/    # NotificationProvider abstraction
├── file/            # FileStorage abstraction
├── event/           # Domain event contracts
└── observability/   # Health, tracing, MDC
```

**Reuse from LocalBoost (copy + adapt, do not import):**
- `TenantContext`, `JwtAuthenticationFilter`, `JwtService` pattern
- `ApiError`, `GlobalExceptionHandler`, `FixnaException`
- `RequestIdFilter`, `LoggingContext`, `SensitiveDataMasker`
- `PlatformHealthService`
- `NonNegotiablesComplianceTest` and tenant sweep tests

**New for Consulting:**
- Permission-based RBAC (not role-enum only)
- `ClientPortalMembership` auth path (future Phase 7)

**Database:** `V1__platform_core.sql` — tenants, users, memberships, roles, permissions, audit_logs, refresh_tokens.

### Phase 2: Consulting CRM

- Client, ClientContact, ClientPortalMembership
- Consulting Profile, ConsultingService
- API: `/api/v1/clients`, `/api/v1/services`
- Frontend: `/clients` route in `frontend/consulting-web`

### Phase 3–10

Follow master prompt phases 3–10 sequentially:
- Phase 3: Engagement, Project, Milestone, Task
- Phase 4: Meeting, CalendarProvider
- Phase 5: Proposal, ProposalItem, lifecycle
- Phase 6: Invoice, InvoiceLine, Payment, PaymentProvider
- Phase 7: Client Portal
- Phase 8: Public Website
- Phase 9: AI assistance
- Phase 10: Hardening, security, deployment

### Package Extraction (ongoing, not blocking)

Extract to `packages/` when the same code is needed in 2+ apps:
1. `@fixna/types` — first (shared TypeScript types)
2. `@fixna/api-client` — second (HTTP client + error schema)
3. `@fixna/ui` — third (shared components)
4. Others as needed

---

## 20. Files / Modules That Should NOT Be Touched Initially

### Do not touch (LocalBoost live production)

| Path | Reason |
|------|--------|
| `fixna-localboost/backend/` | Live API at `api.fixna.in` |
| `fixna-localboost/frontend/` | Live app at `app.fixna.in` |
| `fixna-localboost/backend/src/main/resources/db/migration/` | Production Flyway history (V1–V9) |
| `fixna-localboost/render.yaml` | Live Render deployment |
| `fixna-localboost/frontend/vercel.json` | Live Vercel deployment |
| `fixna-localboost/.github/workflows/ci.yml` | Live CI pipeline |
| LocalBoost production database (Neon) | Isolated data |
| LocalBoost campaign/audience/lead/creative modules | Product-specific domain |
| LocalBoost ad platform adapters | Product-specific integrations |
| LocalBoost subscription billing | Different billing model |

### Do not populate yet (ecosystem placeholders)

| Path | Reason |
|------|--------|
| `frontend/localboost-web/` | LocalBoost frontend stays in `fixna-localboost` until explicit migration decision |
| `backend/localboost-api/` | LocalBoost API stays in `fixna-localboost` |
| `frontend/hospitality-web/` | Future product — no work until Consulting MVP |
| `backend/hospitality-api/` | Future product |

### Safe to build in immediately

| Path | Purpose |
|------|---------|
| `backend/consulting-api/` | Consulting backend (greenfield) |
| `frontend/consulting-web/` | Consulting frontend (greenfield) |
| `packages/*` | Shared packages (as needed, incrementally) |
| `docs/` | Architecture, ADRs, API docs, implementation guides |

---

## Appendix A: LocalBoost Domain Entities (Reference Only)

These belong to LocalBoost and must not appear in Consulting:

- `Tenant`, `User`, `TenantMembership`, `RefreshToken` → **platform primitives** (reimplement in Consulting, don't share tables)
- `Business`, `BusinessLocation` → LocalBoost SMB model
- `Campaign`, `CampaignOffer`, `CampaignChannel` → LocalBoost advertising
- `Audience`, `GeoTarget`, `Creative` → LocalBoost targeting
- `PlatformConnection` → LocalBoost ad platform integration
- `CampaignMetric`, `Lead` → LocalBoost analytics
- `AiUsage`, `Subscription` → LocalBoost AI/billing
- `AuditLog` → **platform primitive** (reimplement)

## Appendix B: Consulting Domain Entities (To Be Built)

Per master prompt — these do not exist anywhere yet:

- `Client`, `ClientContact`, `ClientPortalMembership`
- `ConsultingService`
- `Proposal`, `ProposalItem`
- `Engagement`
- `Project`, `Milestone`, `Task`
- `Meeting`
- `Invoice`, `InvoiceLine`, `Payment`
- `Document`
- `Testimonial`

---

## Assessment Complete

**Status:** TASK-001 complete. No Phase 1 work has been started.  
**Next recommended task:** Pre-Phase repository bootstrap (git init, workspaces, ADRs, CI) followed by Phase 1 Platform Foundation in `backend/consulting-api`.
