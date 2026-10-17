# Hospitality — Deployment

Two independent deploy units + a dedicated database.

| Unit | Path | Platform |
|------|------|----------|
| API | `backend/hospitality-api` (Dockerfile) | Render (`render-hospitality.yaml`) |
| Web | `frontend/hospitality-web` | Vercel (root `frontend/hospitality-web`) |

## Production URLs

| Surface | URL |
|---------|-----|
| Web | https://hospitality.fixna.in |
| API | https://api.hospitality.fixna.in/api |

## Local run

```bash
docker compose up -d postgres-hospitality   # Postgres on localhost:5436, db hospitality
mvn -f backend/hospitality-api/pom.xml spring-boot:run   # API on :8084
npm --prefix frontend/hospitality-web run dev            # web on :3005
```

`.env.local` (web): `NEXT_PUBLIC_API_BASE_URL=http://localhost:8084/api`

## API — Render

Blueprint: `render-hospitality.yaml`. Creates web service
`fixna-hospitality-api` + managed DB `fixna-hospitality-db`
(database `hospitality`). Set these env vars (mark as secret):

- `FIXNA_JWT_SECRET` — ≥32 bytes, unique to this product.
- `FIXNA_CORS_ALLOWED_ORIGINS` — the web origin, e.g. `https://hospitality.fixna.in`.

`SPRING_DATASOURCE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` are wired from the
managed DB. `FIXNA_HSTS_ENABLED=true` in prod. Health check `/api/v1/health`.

`REDIS_URL` is optional: no hospitality feature uses Redis yet, and the Redis
health contributor is disabled (`management.health.redis.enabled=false`), so
deploys pass on instances without a Redis service. Set `REDIS_URL` only when a
Redis instance is provisioned — otherwise the health check would report 503
(connection refused to `localhost:6379`) and fail the deploy.

## Web — Vercel

- Root directory: `frontend/hospitality-web`, framework preset **Next.js**.
- Env: `NEXT_PUBLIC_API_BASE_URL=https://api.hospitality.fixna.in/api`,
  `NEXT_PUBLIC_APP_ENV=production`. The value **must include `https://`** —
  `NEXT_PUBLIC_*` vars are baked into the client bundle at build time, and a
  schemeless host is resolved by the browser as a *path* on the web origin.
- Target domain (verify DNS before use): `hospitality.fixna.in`.

### Troubleshooting — login calls hit the web host instead of the API

Symptom: network tab shows
`https://hospitality.fixna.in/fixna-hospitality.onrender.com/api/v1/auth/login`
(HTTP 400). Cause: `NEXT_PUBLIC_API_BASE_URL` on Vercel is set to the Render
hostname **without** `https://` (e.g. `fixna-hospitality.onrender.com/api`).
Fix: set it to `https://api.hospitality.fixna.in/api` and **redeploy** (env
vars are only re-read at build time). `src/lib/api-base.ts` now prepends
`https://` to schemeless values as a safety net, but the full URL is the
canonical configuration. If the corrected cross-origin call is then blocked,
check `FIXNA_CORS_ALLOWED_ORIGINS` on the Render API includes
`https://hospitality.fixna.in`.

## Verify

- `GET /api/v1/health` → 200 `{status: UP}`.
- Register → login → access operations, guest experiences, service workflows, business configuration.
- Confirm no secrets in logs and CORS rejects other origins.