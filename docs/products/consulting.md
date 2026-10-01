# Consulting

Consulting firm operations platform — CRM through invoicing, client portal, and public website.

## Paths

| Layer | Location |
|-------|----------|
| API | `backend/consulting-api` |
| Web | `frontend/consulting-web` |
| Deploy | `render-consulting.yaml` |
| Requirements | [`docs/implementation/`](../implementation/) |

## Status

MVP v1.0.0 — Phases 1–10 complete (136 API tests).

## Planned production

| Surface | URL |
|---------|-----|
| Web | https://consulting.fixna.in |
| API | https://api.consulting.fixna.in/api |

## Local run

```bash
docker compose up -d
npm run bootstrap:maven
mvn -f backend/consulting-api/pom.xml spring-boot:run    # :8081
npm --prefix frontend/consulting-web run dev             # :3001
```

Env for web:

```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8081/api
NEXT_PUBLIC_APP_ENV=local
```

## Build & test

```bash
npm run test:consulting-api
npm run build:consulting
```

## Domain ownership

Clients, engagements, projects, tasks, milestones, meetings, proposals, invoices, portal, public site, consulting AI (mock provider).

See [architecture/product-boundaries.md](../architecture/product-boundaries.md).
