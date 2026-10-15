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

## Web — Vercel

- Root directory: `frontend/hospitality-web`, framework preset **Next.js**.
- Env: `NEXT_PUBLIC_API_BASE_URL=https://<api-host>/api`, `NEXT_PUBLIC_APP_ENV=production`.
- Target domain (verify DNS before use): `hospitality.fixna.in`.

## Verify

- `GET /api/v1/health` → 200 `{status: UP}`.
- Register → login → access operations, guest experiences, service workflows, business configuration.
- Confirm no secrets in logs and CORS rejects other origins.