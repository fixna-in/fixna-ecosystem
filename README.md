# Fixna Ecosystem — Multi-Product Monorepo

Independent products sharing docs, CI, and Java platform code. Each product deploys as its own **API + web + database** — no shared runtime or merged migrations.

## Architecture

```text
                         FIXNA ECOSYSTEM
                               |
             +-----------------+------------------+
             |                 |                  |
        LocalBoost         Consulting        Hospitality
        Web + API          Web + API          Web + API
             |                 |                  |
        LocalBoost DB     Consulting DB      Hospitality DB
             \________________|_________________/
                               |
              backend/libs/fixna-platform-common
              (logging, security, CORS, errors — Java only)
```

| Principle | Detail |
|-----------|--------|
| **Isolation** | Separate PostgreSQL database and Flyway history per product — never merge migrations |
| **Tenancy** | Multi-tenant; tenant ID from auth context, never from client input |
| **Deploy units** | API on Render (Docker); web on Vercel; DB on Render Postgres or Neon |
| **Shared code** | Java platform lib only today; frontends are product-specific |

### Repository layout

```
fixna-ecosystem/
├── backend/
│   ├── libs/fixna-platform-common/   # Shared Java infrastructure
│   ├── localboost-api/               # LocalBoost Spring Boot API
│   ├── consulting-api/               # Consulting MVP v1.0.0
│   └── hospitality-api/              # placeholder
├── frontend/
│   ├── localboost-web/               # Next.js admin app
│   ├── consulting-web/               # Next.js admin + portal + public site
│   └── hospitality-web/              # placeholder
├── docs/                             # Architecture, deployment, requirements
├── scripts/                          # Dev lifecycle scripts
├── tools/                            # SQL, demo utilities
├── ai/                               # AI prompts/schemas (LocalBoost)
├── docker-compose.yml                # Local Postgres + Redis + Mailhog
├── render-localboost.yaml            # Render blueprint — LocalBoost API
└── render-consulting.yaml            # Render blueprint — Consulting API
```

Layer pattern for each product: `backend/{product}-api` + `frontend/{product}-web` + `render-{product}.yaml`. See [`docs/scaffolding.md`](docs/scaffolding.md).

---

## Products & technology

| Product | Domain | Backend | Frontend | Status |
|---------|--------|---------|----------|--------|
| **LocalBoost** | Campaigns, businesses, leads, analytics | Spring Boot 3.5.6, Java 21, Flyway, Redis | Next.js 16, React 19, TanStack Query | ✅ In monorepo; prod still on standalone repo until cutover |
| **Consulting** | CRM, projects, meetings, proposals, invoicing, portal, public site, AI | Spring Boot 3.5.6, Java 21, Flyway | Next.js 16, React 19, TanStack Query | ✅ MVP v1.0.0 (Phases 1–10) |
| **Hospitality** | _(future)_ | placeholder | placeholder | — |

### Backend stack (all APIs)

- **Java 21** · **Spring Boot 3.5.6** · **PostgreSQL 17** · **Flyway 11.20**
- REST under `/api/v1/*` · JWT auth · RBAC · tenant-scoped data access
- Shared lib: [`backend/libs/fixna-platform-common`](backend/libs/fixna-platform-common) — request IDs, CORS, security headers, global errors, password config

### Frontend stack (all web apps)

- **Next.js 16.3.6** · **React 19** · **TypeScript 5**
- **TanStack Query** · **Axios** · **React Hook Form** · **Zod**
- npm workspaces at repo root (`frontend/*`); hoisted deps in root `node_modules/` (gitignored)

---

## URLs

### Production

| Product | Web (UI) | API base | Health |
|---------|----------|----------|--------|
| **LocalBoost** | https://localboost.fixna.in | https://api.localbost.fixna.in/api | https://api.localbost.fixna.in/api/v1/health |
| **Consulting** | https://consulting.fixna.in _(planned)_ | https://api.consulting.fixna.in/api _(planned)_ | `/api/v1/health/ready` |

LocalBoost production currently runs from the standalone `fixna-localboost` repo; ecosystem copy is ready for cutover.

### Custom domain and CNAME configuration

Configure the following DNS records in the domain provider:

| Service | Hostname | Target | DNS record |
|---------|----------|--------|------------|
| LocalBoost web | `localboost.fixna.in` | Vercel or frontend deployment hostname | `CNAME` |
| LocalBoost API | `api.localbost.fixna.in` | Render web service hostname | `CNAME` |
| Consulting web | `consulting.fixna.in` | Vercel or frontend deployment hostname | `CNAME` |
| Consulting API | `api.consulting.fixna.in` | Render web service hostname | `CNAME` |

The target for each `CNAME` is the hostname provided by the deployment platform, for example `fixna-localboost-api.onrender.com` or `fixna-consulting-api.onrender.com`. Do not use an IP address as the target.

Render setup:

1. Open the Render web service and select **Settings → Custom Domains**.
2. Add the API hostname, for example `api.localbost.fixna.in`.
3. Copy the value shown by Render for the domain verification record.
4. Add the returned verification record in the DNS provider.
5. Repeat for the additional API or web hostname.
6. Wait for DNS propagation and confirm that the domain shows HTTPS in Render.

Vercel setup:

1. Open the project and select **Settings → Domains**.
2. Add the web hostname, for example `localboost.fixna.in` or `consulting.fixna.in`.
3. Add the DNS records supplied by Vercel.
4. Wait for DNS verification before switching traffic to the new domain.

After the DNS records are verified, update the deployed application and frontend environment variables to use the final custom domains instead of the platform-provided hostnames.

### Health API

| Endpoint | Service | Response | Status |
|----------|---------|----------|--------|
| `/api/v1/health` | LocalBoost and Consulting | Aggregated component health, application version, and deployment timestamp | `200` when healthy; otherwise `503` |
| `/api/v1/health/readiness` | LocalBoost | Deployment readiness | `200` when ready; otherwise `503` |
| `/api/v1/health/ready` | Consulting | Deployment readiness | `200` when ready; otherwise `503` |
| `/actuator/health` | LocalBoost and Consulting | Spring Boot actuator health with component details when enabled | `200` or `503` |

Example:

```bash
curl -i http://localhost:8080/api/v1/health/readiness
curl -i http://localhost:8081/api/v1/health/ready
curl -i http://localhost:8080/actuator/health
```

The readiness endpoints return a JSON body such as `{"status":"READY","service":"fixna-localboost-backend","timestamp":"..."}`. Use the aggregated health endpoint for operational status and the readiness endpoint for load balancer or deployment checks.

### Local development

| Product | Web (UI) | API base | Default DB |
|---------|----------|----------|------------|
| **LocalBoost** | http://localhost:3000 | http://localhost:8080/api | `localboost` @ localhost:**5432** |
| **Consulting** | http://localhost:3001 | http://localhost:8081/api | `consulting` @ localhost:**5432** |

**Optional containerized infrastructure** (from `docker compose up -d`):

| Service | URL / port |
|---------|------------|
| Redis | localhost:6379 |
| Mailhog UI | http://localhost:8025 |
| Mailhog SMTP | localhost:1025 |

### UI routes

**LocalBoost** (`frontend/localboost-web`)

| Route | Purpose |
|-------|---------|
| `/`, `/login`, `/register` | Landing & auth |
| `/dashboard` | Overview |
| `/businesses`, `/businesses/[id]` | Business management |
| `/campaigns`, `/campaigns/new`, `/campaigns/[id]` | Campaigns |
| `/leads` | Lead inbox |

**Consulting** (`frontend/consulting-web`)

| Route | Purpose |
|-------|---------|
| `/`, `/login`, `/register` | Admin landing & auth |
| `/clients` | CRM |
| `/projects` | Engagements, milestones, tasks |
| `/meetings` | Calendar / meetings |
| `/proposals` | Proposals |
| `/invoices` | Invoicing & payments |
| `/website` | Public site admin |
| `/site/[slug]` | Published tenant website |
| `/portal` | Client portal |
| `/ai` | AI assistant (mock provider) |

### API surface (Consulting)

All authenticated routes use `/api/v1/*` unless noted.

| Prefix | Area |
|--------|------|
| `/api/v1/auth` | Staff login, refresh |
| `/api/v1/clients` | CRM |
| `/api/v1/services` | Service catalog |
| `/api/v1/profile` | Tenant profile |
| `/api/v1/meetings` | Meetings |
| `/api/v1/proposals`, `/invoices`, `/projects`, `/engagements` | Delivery & billing |
| `/api/v1/portal`, `/api/v1/portal/auth` | Client portal |
| `/api/v1/public/sites` | Public website (unauthenticated) |
| `/api/v1/ai` | AI assistant |
| `/api/v1/health`, `/actuator/health` | Health & readiness |

LocalBoost API follows the same `/api/v1/*` convention (campaigns, businesses, leads, auth, health). OpenAPI available in non-prod profiles.

---

## Build, test & run

### Prerequisites

- **Java 21** · **Maven 3.9+** · **Node.js 20+** · Docker (optional)

### 1. Optional containerized infrastructure

The APIs and web apps run as local processes and connect to native PostgreSQL.
Use Compose only if you also need the containerized Redis or Mailhog services:

```bash
docker compose up -d
```

### 2. Environment variables

Create product-specific files before running the apps. Never commit real secrets or production credentials.

#### LocalBoost API (`backend/localboost-api`)

| Variable | Local value | Production value |
|----------|-------------|------------------|
| `SPRING_PROFILES_ACTIVE` | `local` | `prod` |
| `SERVER_PORT` | `8080` | Render `PORT` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/fixna` | Render PostgreSQL connection string |
| `POSTGRES_USER` | `fixna` | Render PostgreSQL user |
| `POSTGRES_PASSWORD` | `change-me` | Render PostgreSQL password |
| `FIXNA_JWT_SECRET` | Long random secret | Render secret, never committed |
| `FIXNA_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://127.0.0.1:3000` | `https://localboost.fixna.in` |
| `FIXNA_APP_ENV` | `local` | `prod` |
| `FIXNA_AI_PROVIDER` | `mock` | `mock` |
| `FIXNA_PLATFORM_MODE` | `mock` | `mock` |
| `FIXNA_BILLING_DEFAULT_PLAN` | `FREE` | `STARTER` |
| `REDIS_URL` | `redis://localhost:6379` | Redis service URL or omitted when unavailable |
| `FIXNA_HSTS_ENABLED` | `false` | `true` |
| `OTEL_EXPORTER_OTLP_ENABLED` | `false` | `false` |
| `FIXNA_JWT_ACCESS_TTL` | `PT15M` | `PT15M` |
| `FIXNA_JWT_REFRESH_TTL` | `P7D` | `P7D` |
| `FIXNA_RATE_LIMIT_PER_IP_PER_MINUTE` | `20` | `20` |
| `DB_POOL_MAX` | `10` | `10` |
| `DB_POOL_MIN` | `2` | `2` |
| `DB_CONN_TIMEOUT_MS` | `5000` | `5000` |

#### Consulting API (`backend/consulting-api`)

| Variable | Local value | Production value |
|----------|-------------|------------------|
| `SPRING_PROFILES_ACTIVE` | `local` | `prod` |
| `SERVER_PORT` | `8081` | Render `PORT` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/consulting` | Render PostgreSQL connection string |
| `POSTGRES_USER` | `postgres` | Render PostgreSQL user |
| `POSTGRES_PASSWORD` | `change-me` | Render PostgreSQL password |
| `FIXNA_JWT_SECRET` | Long random secret | Render secret, never committed |
| `FIXNA_CORS_ALLOWED_ORIGINS` | `http://localhost:3001,http://127.0.0.1:3001` | `https://fixna-ecosystem-consulting.vercel.app,https://consulting.fixna.in` |
| `FIXNA_APP_ENV` | `local` | `prod` |
| `FIXNA_HSTS_ENABLED` | `false` | `true` |
| `FIXNA_FILE_STORAGE_PATH` | `./data/files` | Persistent storage path provided by the deployment |
| `FIXNA_JWT_ACCESS_TTL` | `PT15M` | `PT15M` |
| `FIXNA_JWT_REFRESH_TTL` | `P7D` | `P7D` |
| `DB_POOL_MAX` | `10` | `10` |
| `DB_POOL_MIN` | `2` | `2` |
| `DB_CONN_TIMEOUT_MS` | `5000` | `5000` |
| `FIXNA_TEST_DATA_ENABLED` | `false` | `false` |
| `FIXNA_TEST_USER_PASSWORD` | Empty | Empty |
| `FIXNA_DEPLOYED_AT` | Empty | Optional ISO-8601 deployment timestamp |

#### LocalBoost web (`frontend/localboost-web`)

| Variable | Local value | Production value |
|----------|-------------|------------------|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080/api` | `https://localboost-api.fixna.in/api` |
| `NEXT_PUBLIC_APP_ENV` | `local` | `prod` |

#### Consulting web (`frontend/consulting-web`)

| Variable | Local value | Production value |
|----------|-------------|------------------|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8081/api` | `https://api.consulting.fixna.in/api` |
| `NEXT_PUBLIC_APP_ENV` | `local` | `prod` |

> The current Consulting API client adds `/v1` automatically. For other clients, the API URL may be configured with `/api/v1` instead.

#### Environment templates

A complete root template is available at [`\.env.example`](.env.example). For LocalBoost web, use [`frontend/localboost-web/.env.example`](frontend/localboost-web/.env.example).

### 3. Bootstrap Maven (once per clone, or after platform-common changes)

Product APIs depend on `fixna-platform-common:1.0.0`. Install it before building a single product:

```bash
npm run bootstrap:maven
# or: scripts/bootstrap-maven.sh  (scripts/bootstrap-maven.cmd on Windows)
```

### 4. Install frontend dependencies (once)

```bash
npm install
```

### 5. Run APIs

```bash
# LocalBoost — port 8080, DB localhost:5432/fixna
mvn -f backend/localboost-api/pom.xml spring-boot:run

# Consulting — port 8081, native PostgreSQL localhost:5432/consulting
SPRING_PROFILES_ACTIVE="local" POSTGRES_USER="postgres" POSTGRES_PASSWORD="admin" mvn -f backend/consulting-api/pom.xml spring-boot:run
```

### 6. Run web apps

Copy env files before first run:

```bash
# LocalBoost — see frontend/localboost-web/.env.example
cp frontend/localboost-web/.env.example frontend/localboost-web/.env.local

# Consulting
echo NEXT_PUBLIC_API_BASE_URL=http://localhost:8081/api > frontend/consulting-web/.env.local
echo NEXT_PUBLIC_APP_ENV=local >> frontend/consulting-web/.env.local
```

```bash
npm --prefix frontend/localboost-web run dev      # http://localhost:3000
npm --prefix frontend/consulting-web run dev      # http://localhost:3001
```

### Build & test (all products)

```bash
mvn -f pom.xml clean test                         # 372 tests (229 LocalBoost + 136 Consulting + 7 platform-common)
npm run build:all                                 # localboost-web + consulting-web
npm run test:all-apis                             # Maven tests per API
npm run package:all-apis                          # JAR packages
```

| Script | Action |
|--------|--------|
| `npm run bootstrap:maven` | Install parent POM + platform-common to local `.m2` |
| `npm run build:localboost` | Production build — LocalBoost web |
| `npm run build:consulting` | Production build — Consulting web |
| `npm run test:localboost-api` | LocalBoost API tests |
| `npm run test:consulting-api` | Consulting API tests |

| Module | API tests |
|--------|-----------|
| LocalBoost | 229 |
| Consulting | 136 |
| Platform common | 7 |
| **Total** | **372** |

---

## Deployment

Each product ships independently.

| Layer | LocalBoost | Consulting |
|-------|------------|------------|
| **API** | Render — [`render-localboost.yaml`](render-localboost.yaml) | Render — [`render-consulting.yaml`](render-consulting.yaml) |
| **Web** | Vercel — root `frontend/localboost-web` | Vercel — root `frontend/consulting-web` |
| **Database** | Render Postgres or Neon (`fixna`) | Render Postgres (`consulting`) |
| **Region** | Singapore | Oregon |

### API deploy (Docker)

```bash
# Both use monorepo context (includes fixna-platform-common)
docker build -f backend/localboost-api/Dockerfile -t fixna-localboost-api .
docker build -f backend/consulting-api/Dockerfile -t fixna-consulting-api .
```

Required production env vars: `SPRING_PROFILES_ACTIVE=prod`, `FIXNA_JWT_SECRET`, `FIXNA_CORS_ALLOWED_ORIGINS`, `SPRING_DATASOURCE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.

### Web deploy (Vercel)

| Product | Root directory | `NEXT_PUBLIC_API_BASE_URL` |
|---------|----------------|----------------------------|
| LocalBoost | `frontend/localboost-web` | `https://api.fixna.in/api` |
| Consulting | `frontend/consulting-web` | `https://api.consulting.fixna.in/api` |

Full checklists: [`docs/deployment/localboost.md`](docs/deployment/localboost.md) · [`docs/deployment/consulting.md`](docs/deployment/consulting.md)

---

## Documentation

| Topic | Location |
|-------|----------|
| Agent guide | [`AGENTS.md`](AGENTS.md) |
| Architecture | [`docs/architecture.md`](docs/architecture.md) · [`docs/architecture/`](docs/architecture/) |
| Product guides | [`docs/products/`](docs/products/) |
| Scaffolding new products | [`docs/scaffolding.md`](docs/scaffolding.md) |
| Database migrations | [`docs/database/README.md`](docs/database/README.md) |
| Deployment | [`docs/deployment/`](docs/deployment/) |
| Memory bank (agent context) | [`docs/memory-bank/`](docs/memory-bank/) |
| Changelog | [`docs/CHANGELOG.md`](docs/CHANGELOG.md) |

**Rule:** each product has its own Flyway migration history — **never merge migrations** between `consulting-api` and `localboost-api`.
