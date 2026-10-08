# Fixna Ecosystem — Project State Handover (no secrets)

**Purpose:** Paste into ChatGPT or another assistant for complete context about the Fixna monorepo.
**Last updated:** 2026-10-08
**Version:** 1.0.0
**Products:** Consulting, LocalBoost, Hospitality (planned)

---

## 1. Executive summary

Fixna is a multi-product ecosystem of independently deployable business applications. Each product owns its own Next.js frontend, Spring Boot API, PostgreSQL database, Flyway migrations, and deployment configuration. Shared Java infrastructure is limited to `backend/libs/fixna-platform-common`.

Current products:

- **Consulting** — client CRM, projects, meetings, proposals, invoicing, client portal, and public websites.
- **LocalBoost** — local-business campaigns, leads, audiences, creatives, analytics, and advertising-platform adapters.
- **Hospitality** — planned future product; no active implementation is present.

Core rules:

- One modular monolith per product.
- No product-to-product imports or shared domain models.
- Shared infrastructure only: web, security, logging, configuration, and auto-configuration.
- Separate PostgreSQL databases and Flyway histories.
- Independent API and frontend deployments.
- Tenant identity is resolved server-side; client-supplied tenant IDs are never trusted.
- AI output is untrusted and must be validated before use.
- Schema changes require a new migration in the owning product.
- AI must not spend money, launch campaigns, or perform external actions without explicit user approval.

---

## 2. Current product status

| Product | Version | Status | API | Web | Database | Deployment status |
|---|---:|---|---|---|---|---|
| Consulting | 1.0.0 | MVP complete and branded | `backend/consulting-api` | `frontend/consulting-web` | `consulting` | Render configuration exists; production target is `consulting.fixna.in` |
| LocalBoost | 1.0.0 | Ecosystem-ready; production cutover pending | `backend/localboost-api` | `frontend/localboost-web` | `fixna` | Live at `app.fixna.in` and `api.fixna.in`; currently deploys from the standalone repository |
| Hospitality | Not started | Planned | `backend/hospitality-api` | `frontend/hospitality-web` | Not provisioned | No active deployment |

### Test status

| Area | Result |
|---|---|
| Consulting API | 136 tests |
| LocalBoost API | 229 tests |
| Platform common | 7 tests |
| LocalBoost + platform | 236 total tests |
| Consulting web | Production build completed |
| LocalBoost web | TypeScript integration verified; full build output was interrupted by the terminal environment |

---

## 3. Ecosystem architecture

```text
Fixna Ecosystem
├── Consulting         Web + API + PostgreSQL
├── LocalBoost         Web + API + PostgreSQL
├── Hospitality        Future product
└── fixna-platform-common
```

### Dependency direction

```text
fixna-platform-common
        ↑
  consulting-api
  localboost-api
```

The shared platform library never imports product domain code. Product APIs may use shared infrastructure, but business logic remains inside each product.

---

## 4. Product boundaries

### Consulting

Owns clients, engagements, projects, tasks, milestones, meetings, proposals, invoices, client portal, public websites, and consulting-specific AI.

### LocalBoost

Owns businesses, campaigns, audiences, creatives, budgets, leads, analytics, subscription billing foundations, local-business AI, and advertising-platform adapters.

### Hospitality

The hospitality product is planned but not implemented. Do not add hospitality code or migrations to Consulting or LocalBoost.

### Shared platform

`backend/libs/fixna-platform-common` contains only shared infrastructure:

- API error handling
- Global exception handling
- Request ID propagation
- Security headers
- CORS configuration
- Password encoder configuration
- MDC logging and masking
- Tenant-scope bridge interfaces
- Spring Boot auto-configuration

It does not contain product entities, repositories, RBAC, AI logic, billing rules, controllers, or Flyway migrations.

---

## 5. Repository layout

```text
fixna-ecosystem/
├── AGENTS.md
├── package.json
├── pom.xml
├── docker-compose.yml
├── render-localboost.yaml
├── render-consulting.yaml
├── backend/
│   ├── consulting-api/
│   ├── hospitality-api/
│   └── libs/fixna-platform-common/
├── frontend/
│   ├── consulting-web/
│   ├── hospitality-web/
│   └── localboost-web/
├── packages/fixna-brand/
├── docs/
│   ├── architecture/
│   ├── database/
│   ├── decisions/
│   ├── deployment/
│   ├── implementation/
│   ├── memory-bank/
│   └── products/
├── ai/
├── infrastructure/
├── scripts/
├── services/
├── tools/
└── .cursor/products/
```

### Product locations

| Area | Consulting | LocalBoost |
|---|---|---|
| API | `backend/consulting-api` | `backend/localboost-api` |
| Web | `frontend/consulting-web` | `frontend/localboost-web` |
| Product docs | `docs/products/consulting.md` | `docs/products/localboost/` |
| Deployment | `render-consulting.yaml` | `render-localboost.yaml` |
| Flyway | `backend/consulting-api/src/main/resources/db/migration` | `backend/localboost-api/src/main/resources/db/migration` |
| Product-specific AI | `backend/consulting-api` | `ai/localboost/` |

---

## 6. Technology stack

### Web

- Next.js 16.3.6
- React 19
- TypeScript
- TanStack Query
- React Hook Form
- Zod
- Axios

### Backend

- Java 21
- Spring Boot 3.x
- Maven
- PostgreSQL
- Flyway
- Spring Security
- JWT
- Redis where product-specific configuration enables it
- Micrometer and tracing

### Shared branding

- Shared workspace package: `@fixna/brand`
- Canonical artwork: `packages/fixna-brand/src/index.tsx`
- Consulting wrapper: `frontend/consulting-web/src/brand/brand-mark-icon.tsx`
- LocalBoost wrapper: `frontend/localboost-web/src/brand/brand-mark-icon.tsx`
- Favicon assets: `frontend/{product}-web/src/app/icon.svg`
- Apple icons: `frontend/{product}-web/src/app/apple-icon.svg`

The shared package is the source of truth for future visual changes. Product favicon files are synchronized with it.

---

## 7. Local development

### Prerequisites

- JDK 21
- Node.js and npm
- Docker Desktop or Docker Engine

### Start infrastructure

```bash
docker compose up -d
```

### Bootstrap Maven

```bash
npm run bootstrap:maven
```

### Consulting

```bash
mvn -f backend/consulting-api/pom.xml spring-boot:run
npm --prefix frontend/consulting-web run dev
```

- Web: http://localhost:3001
- API: http://localhost:8081
- Web environment: `NEXT_PUBLIC_API_BASE_URL=http://localhost:8081/api`
- App environment: `NEXT_PUBLIC_APP_ENV=local`

### LocalBoost

```bash
mvn -f backend/localboost-api/pom.xml spring-boot:run
npm --prefix frontend/localboost-web run dev
```

- Web: http://localhost:3000
- API: http://localhost:8080
- Web environment: copy `frontend/localboost-web/.env.example` to `.env.local`

### Full build and test

```bash
mvn -f pom.xml clean test
npm run build:all
npm run test:all-apis
```

---

## 8. Database and migrations

### Current databases

| Product | Database | Migration location |
|---|---|---|
| Consulting | `consulting` | `backend/consulting-api/src/main/resources/db/migration` |
| LocalBoost | `fixna` | `backend/localboost-api/src/main/resources/db/migration` |

Do not merge Flyway histories or copy migrations between products.

### Migration rules

- Every schema change requires a new migration.
- Never edit an applied production migration.
- Keep backwards compatibility for existing data.
- Use product-specific migration scripts.
- LocalBoost demo SQL remains under `tools/localboost/sql/`.

---

## 9. Deployment

### Consulting

- API: `render-consulting.yaml`
- Web: Vercel
- Current web target: https://consulting.fixna.in
- Current API target: https://api.consulting.fixna.in/api

### LocalBoost

- API: `render-localboost.yaml`
- Web: Vercel
- Current web target: https://app.fixna.in
- Current API target: https://api.fixna.in/api
- Production currently deploys from the standalone `fixna-localboost` repository.

### Deployment rules

- Product API and web deploy separately.
- Each deployment has its own environment variables.
- Never commit secrets or live environment values.
- Do not share a database or migration history across products.

---

## 10. Security and data handling

### Non-negotiables

- Never trust a client-supplied tenant ID.
- Resolve tenant identity only from the authenticated context.
- Never log passwords, tokens, secrets, or JWTs.
- Never expose secrets in code, documentation, screenshots, or logs.
- Validate all AI output before storing or acting on it.
- AI cannot spend money or initiate external actions without explicit approval.
- Use BCrypt password hashes only.
- Controllers should not contain business logic.

### Tenant isolation

Every tenant-owned record must be scoped to the authenticated tenant. Filtering belongs in the application and repository layers, not in untrusted request parameters.

---

## 11. Current work and status

### Completed

- Consulting MVP rebuilt after accidental deletion.
- Consulting phases 1–10 were reconstructed.
- Consulting has the Fixna visual design system and shared branding.
- Consulting favicon, Apple icon, and manifest were added.
- Consulting production build was validated.
- LocalBoost and Consulting versions are aligned at 1.0.0.
- Shared Fixna brand package was created and linked to both web applications.
- Static favicon and Apple icons were synchronized.
- Platform, deployment, architecture, and product-boundary documentation were reviewed.

### Pending

1. LocalBoost production cutover from the standalone repository.
2. Final production configuration review for both Render services.
3. Vercel production deployment verification for Consulting and LocalBoost.
4. End-to-end LocalBoost smoke tests.
5. Consulting integrations beyond the MVP.
6. Hospitality product implementation.
7. Initial Git commit to protect the ecosystem state.

---

## 12. Environment variables

### Shared names

- `NEXT_PUBLIC_API_BASE_URL`
- `NEXT_PUBLIC_APP_ENV`
- `FIXNA_JWT_SECRET`
- `FIXNA_CORS_ALLOWED_ORIGINS`
- `FIXNA_APP_ENV`
- `FIXNA_AI_PROVIDER`
- `FIXNA_PLATFORM_MODE`
- `FIXNA_DEPLOYED_AT`

### Consulting API

- `SPRING_PROFILES_ACTIVE`
- `SPRING_DATASOURCE_URL`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `FIXNA_JWT_SECRET`
- `FIXNA_CORS_ALLOWED_ORIGINS`
- `FIXNA_APP_ENV`
- `FIXNA_AI_PROVIDER`
- `FIXNA_PLATFORM_MODE`

### LocalBoost API

- `SPRING_PROFILES_ACTIVE`
- `SPRING_DATASOURCE_URL`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `FIXNA_JWT_SECRET`
- `FIXNA_CORS_ALLOWED_ORIGINS`
- `FIXNA_APP_ENV`
- `FIXNA_AI_PROVIDER`
- `FIXNA_PLATFORM_MODE`
- `FIXNA_DEPLOYED_AT`

Never commit real values. Use `.env.example` files or a secret manager.

---

## 13. Repository rules

- Never merge Flyway migrations between products.
- Never import LocalBoost domain code into Consulting, or vice versa.
- Never trust client-supplied tenant IDs.
- Do not merge product deployments.
- Preserve independent deployability.
- Add a new migration for every schema change.
- Do not introduce dependencies without documenting why.
- Keep product-specific documentation in product-specific locations.
- Validate AI output before use.
- Preserve current production deployments unless cutover is explicitly approved.

---

## 14. Current checklist

- [x] Ecosystem architecture and product boundaries are documented.
- [x] Consulting MVP is rebuilt and versioned at 1.0.0.
- [x] LocalBoost product metadata is versioned at 1.0.0.
- [x] Shared Fixna branding package exists.
- [x] Consulting and LocalBoost icons are synchronized.
- [x] Consulting production build compiles and type-checks.
- [x] LocalBoost shared package resolution is verified.
- [x] Consulting and LocalBoost TypeScript checks report no diagnostics.
- [x] Platform common and LocalBoost API tests are available.
- [x] Consulting API tests are available.
- [x] LocalBoost live deployment remains available.
- [ ] LocalBoost production cutover is complete.
- [ ] Consulting production deployment is validated.
- [ ] LocalBoost end-to-end smoke tests are complete.
- [ ] Initial Git commit is created.
- [ ] Hospitality implementation starts.

---

## 15. Canonical documentation map

| Topic | Location |
|---|---|
| Ecosystem overview | `docs/architecture/ecosystem.md` |
| Product boundaries | `docs/architecture/product-boundaries.md` |
| Shared platform | `docs/architecture/shared-platform.md` |
| Database isolation | `docs/architecture/database-isolation.md` |
| Deployment | `docs/architecture/deployment.md` |
| Consulting product | `docs/products/consulting.md` |
| LocalBoost product | `docs/products/localboost/` |
| Ecosystem changelog | `docs/CHANGELOG.md` |
| Active context | `docs/memory-bank/activeContext.md` |
| LocalBoost context | `docs/memory-bank/localboost-activeContext.md` |
| This handover | `docs/memory-bank/localboost-chatgpt-handoff.md` |
| Scaffolding | `docs/scaffolding.md` |
| Database index | `docs/database/README.md` |
| ADRs | `docs/decisions/` |

---

## 16. Handover guidance

1. Read relevant product rules under `.cursor/products/{product}/rules/` before editing.
2. Read only the needed implementation and architecture documents.
3. Preserve multi-tenancy and database isolation.
4. Never bypass secret handling.
5. Validate AI output before use.
6. Do not merge product migrations, domains, or deployments.
7. Verify builds with the appropriate product command.
8. Keep implementation and documentation synchronized.

---

## 17. Do not share externally

Do not provide or paste these values into a chat or external system:

- Neon credentials
- JWT secrets
- Database passwords
- API keys or tokens
- Render or Vercel credentials
- Plaintext demo-user passwords
- Production database contents
- Private deployment metadata
