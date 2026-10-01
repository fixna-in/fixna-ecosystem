# Active Context

**Last updated:** 2026-09-28  
**Current focus:** Monorepo stable — both products build and test; LocalBoost production cutover pending.

## Current state

| Product | Status | Tests | Notes |
|---------|--------|-------|-------|
| **Consulting** | ✅ MVP v1.0.0 | 136 API tests | Rebuilt Phases 1–10 after accidental deletion; deploy via `render-consulting.yaml` |
| **LocalBoost** | ✅ Ecosystem-ready | 236 (229 + 7 platform-common) | Migrated from standalone repo; production cutover pending |

## Monorepo layout

```text
fixna-ecosystem/
├── backend/                    # Java APIs + shared lib
│   ├── libs/fixna-platform-common/
│   └── {product}-api/
├── frontend/                   # Next.js apps
│   └── {product}-web/
├── docs/, scripts/, tools/, ai/
├── docker-compose.yml          # Postgres (both products) + Redis
├── render-localboost.yaml
└── render-consulting.yaml
```

| Product | API | Web | Port (local) |
|---------|-----|-----|--------------|
| Consulting | `backend/consulting-api` | `frontend/consulting-web` | API :8081, web :3001 |
| LocalBoost | `backend/localboost-api` | `frontend/localboost-web` | API :8080, web :3000 |

LocalBoost and Consulting use **separate databases and Flyway histories** — never merge migrations.

## Incident (resolved)

During `services/` → `backend/` restructure, Consulting was accidentally deleted. MVP was rebuilt from `docs/implementation/phase-*.md`. Stale `services/` lock artifacts and superseded restore docs were removed.

## Database scripts

- Index: `docs/database/README.md`
- Consulting Flyway: `backend/consulting-api/src/main/resources/db/migration/` (V1–V9)
- LocalBoost Flyway: `backend/localboost-api/src/main/resources/db/migration/` (V1–V9)
- LocalBoost demo SQL: `tools/localboost/sql/`

## Local dev

```bash
docker compose up -d

# LocalBoost
mvn -f backend/localboost-api/pom.xml spring-boot:run
npm --prefix frontend/localboost-web run dev

# Consulting
mvn -f backend/consulting-api/pom.xml spring-boot:run
npm --prefix frontend/consulting-web run dev
```

## Build & verify

```bash
mvn -f pom.xml clean test
npm run build:all
```

Deploy: [`docs/architecture/deployment.md`](../architecture/deployment.md), [`docs/scaffolding.md`](../scaffolding.md), product deployment checklists.

Bootstrap Maven once per clone: `npm run bootstrap:maven`

## Next recommended tasks

1. Initial git commit to protect monorepo state
2. LocalBoost production cutover from standalone `fixna-localboost` repo
3. Post-MVP Consulting integrations (calendar, payments, AI providers)
