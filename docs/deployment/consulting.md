# Consulting deployment checklist

| Unit | Path |
|------|------|
| API | `backend/consulting-api` |
| Web | `frontend/consulting-web` |
| Migrations | `backend/consulting-api/src/main/resources/db/migration/` |

See [`docs/database/README.md`](../database/README.md) for migration index and Docker setup.

## Pre-deploy

- [ ] PostgreSQL database provisioned (isolated from LocalBoost — database `consulting`)
- [ ] `FIXNA_JWT_SECRET` set to a unique 32+ character secret
- [ ] `FIXNA_CORS_ALLOWED_ORIGINS` set to production web origin(s) only
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] Flyway migrations V1–V9 applied (automatic on API startup)
- [ ] File storage path writable (`FIXNA_FILE_STORAGE_PATH` or default `/var/data/files`)

## API (Render)

Blueprint: **`render-consulting.yaml`**

```bash
docker build -f backend/consulting-api/Dockerfile -t fixna-consulting-api .
docker run -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e FIXNA_JWT_SECRET=... \
  -e FIXNA_CORS_ALLOWED_ORIGINS=https://consulting.fixna.in \
  -e SPRING_DATASOURCE_URL=... \
  -e POSTGRES_USER=... \
  -e POSTGRES_PASSWORD=... \
  fixna-consulting-api
```

Health probes:
- Liveness: `GET /actuator/health`
- Readiness: `GET /api/v1/health/readiness`

## Web (Vercel)

- **Root Directory:** `frontend/consulting-web`
- **Framework:** Next.js (see `vercel.json`)
- Env:
  ```
  NEXT_PUBLIC_API_BASE_URL=https://api.consulting.fixna.in/api
  NEXT_PUBLIC_APP_ENV=prod
  ```

```bash
npm run build:consulting
```

## Database migrations

| Version | File |
|---------|------|
| V1 | Platform core (tenants, RBAC, auth) |
| V2 | CRM (clients, engagements) |
| V3 | Projects & tasks |
| V4 | Meetings |
| V5 | Proposals |
| V6 | Invoicing |
| V7 | Client portal |
| V8 | Public website |
| V9 | AI assistance |

Full index: `backend/consulting-api/src/main/resources/db/README.md`

## Post-deploy smoke test

1. `GET /api/v1/health/readiness` → 200 READY
2. Register a workspace user (or use seeded admin)
3. Create client → project → proposal → invoice flow
4. Portal login with granted membership
5. Public site at `/site/{slug}` after publishing in `/website`

## Safety rules

- Never share database or JWT secrets with LocalBoost
- Never deploy swagger UI in production (disabled via `application-prod.yml`)
- AI suggestions remain advisory-only — no autonomous client actions
