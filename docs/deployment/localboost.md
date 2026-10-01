# LocalBoost deployment (fixna-ecosystem)

| Unit | Path |
|------|------|
| API | `backend/localboost-api` |
| Web | `frontend/localboost-web` |
| Migrations | `backend/localboost-api/src/main/resources/db/migration/` |
| Demo SQL | `tools/localboost/sql/` |

## Pre-deploy

- [ ] PostgreSQL provisioned (**separate** from Consulting — database `fixna` or dedicated Neon branch)
- [ ] `FIXNA_JWT_SECRET` — unique 32+ character secret
- [ ] `FIXNA_CORS_ALLOWED_ORIGINS` — production web origin only (e.g. `https://app.fixna.in`)
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] Flyway V1–V9 applied (automatic on API startup)
- [ ] Redis URL set if using rate limiting / sessions in prod

## API (Render)

Blueprint: **`render-localboost.yaml`**

```bash
docker build -f backend/localboost-api/Dockerfile -t fixna-localboost-api .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e FIXNA_JWT_SECRET=... \
  -e FIXNA_CORS_ALLOWED_ORIGINS=https://app.fixna.in \
  -e SPRING_DATASOURCE_URL=... \
  -e POSTGRES_USER=... \
  -e POSTGRES_PASSWORD=... \
  fixna-localboost-api
```

Health probes:
- Liveness: `GET /actuator/health`
- Readiness: `GET /api/v1/health/readiness`

**Neon (existing prod):** keep external DB URL in Render env instead of Render Postgres if preferred.

## Web (Vercel)

- **Root Directory:** `frontend/localboost-web`
- **Framework:** Next.js (see `vercel.json`)
- Env:
  ```
  NEXT_PUBLIC_API_BASE_URL=https://api.fixna.in/api
  NEXT_PUBLIC_APP_ENV=prod
  ```

## Database scripts

| Location | Purpose |
|----------|---------|
| `backend/localboost-api/src/main/resources/db/migration/V*.sql` | Flyway (auto) |
| `backend/localboost-api/src/main/resources/db/seed/local-test-data.sql` | Local profile seed |
| `tools/localboost/sql/neon-demo-seed.sql` | Manual Neon demo load |
| `tools/localboost/sql/schema.sql` | Schema reference |

See [`docs/database/README.md`](../database/README.md).

## Post-deploy smoke test

1. `GET /api/v1/health/readiness` → 200 READY
2. Register tenant / login
3. Create business → campaign → launch (mock platform)
4. View leads and analytics dashboard

## Agent docs & rules

- [`AGENTS.md`](../../AGENTS.md)
- [`.cursor/products/localboost/`](../../.cursor/products/localboost/)
- [`docs/products/localboost/`](../../docs/products/localboost/)
- [`ai/localboost/prompts/`](../../ai/localboost/prompts/)

## Safety rules

- Never share database or JWT secrets with Consulting
- Never deploy swagger UI in production
- AI cannot launch campaigns or spend money autonomously
