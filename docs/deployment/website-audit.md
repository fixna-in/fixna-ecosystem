# Website Audit — Deployment

Two independent deploy units + a dedicated database.

| Unit | Path | Platform |
|------|------|----------|
| API | `backend/website-audit-api` (Dockerfile) | Render (`render-website-audit.yaml`) |
| Web | `frontend/website-audit-web` | Vercel (root `frontend/website-audit-web`) |

## Production URLs

| Surface | URL |
|---------|-----|
| Web | https://websiteaudit.fixna.in |
| API | https://api.websiteaudit.fixna.in/api |

## Local run

```bash
docker compose up -d postgres-website-audit     # Postgres on localhost:5434, db website_audit
mvn -f backend/website-audit-api/pom.xml spring-boot:run   # API on :8082
npm --prefix frontend/website-audit-web run dev            # web on :3003
```

`.env.local` (web): `NEXT_PUBLIC_API_BASE_URL=http://localhost:8082/api`

## API — Render

Blueprint: `render-website-audit.yaml`. Creates web service
`fixna-website-audit-api` + managed DB `fixna-website-audit-db`
(database `website_audit`). Set these env vars (mark as secret):

- `FIXNA_JWT_SECRET` — ≥32 bytes, unique to this product.
- `FIXNA_CORS_ALLOWED_ORIGINS` — the web origin, e.g. `https://websiteaudit.fixna.in`.

`SPRING_DATASOURCE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` are wired from the
managed DB. `FIXNA_HSTS_ENABLED=true` in prod. Health check `/api/v1/health`.

## Web — Vercel

- Root directory: `frontend/website-audit-web`, framework preset **Next.js**.
- Env: `NEXT_PUBLIC_API_BASE_URL=https://<api-host>/api`, `NEXT_PUBLIC_APP_ENV=production`.
- Target domain (verify DNS before use): `websiteaudit.fixna.in`.

## Verify

- `GET /api/v1/health` → 200 `{status: UP}`.
- Register → login → submit an audit job → poll → open report.
- Confirm no secrets in logs and CORS rejects other origins.
