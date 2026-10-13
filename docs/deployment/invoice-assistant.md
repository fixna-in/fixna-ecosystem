# Invoice Assistant — Deployment

Two independent deploy units + a dedicated database.

| Unit | Path | Platform |
|------|------|----------|
| API | `backend/invoice-assistant-api` (Dockerfile) | Render (`render-invoice-assistant.yaml`) |
| Web | `frontend/invoice-assistant-web` | Vercel (root `frontend/invoice-assistant-web`) |

## Local run

```bash
docker compose up -d postgres-invoice-assistant   # Postgres on localhost:5435, db invoice_assistant
mvn -f backend/invoice-assistant-api/pom.xml spring-boot:run   # API on :8083
npm --prefix frontend/invoice-assistant-web run dev            # web on :3004
```

`.env.local` (web): `NEXT_PUBLIC_API_BASE_URL=http://localhost:8083/api`

## File storage — important

Uploaded PDFs are written to local disk (`FIXNA_FILE_STORAGE_PATH`, default
`./data/invoice-files`). A Render web service has an **ephemeral filesystem**:
attach a **persistent disk** (mounted at `/var/data`) and set
`FIXNA_FILE_STORAGE_PATH=/var/data/invoice-files` **before going live**, or
uploads will be lost on redeploy/restart. For production, prefer private object
storage behind the same `FileStorage` interface.

## API — Render

Blueprint: `render-invoice-assistant.yaml`. Creates web service
`fixna-invoice-assistant-api` + managed DB `fixna-invoice-assistant-db`
(database `invoice_assistant`). Set these env vars (mark as secret):

- `FIXNA_JWT_SECRET` — ≥32 bytes, unique to this product.
- `FIXNA_CORS_ALLOWED_ORIGINS` — the web origin, e.g. `https://invoices.fixna.in`.
- `FIXNA_FILE_STORAGE_PATH=/var/data/invoice-files` (with a persistent disk).

`SPRING_DATASOURCE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` are wired from the
managed DB. `FIXNA_HSTS_ENABLED=true` in prod. Health check `/api/v1/health/ready`.

## Web — Vercel

- Root directory: `frontend/invoice-assistant-web`, framework preset **Next.js**.
- Env: `NEXT_PUBLIC_API_BASE_URL=https://<api-host>/api`, `NEXT_PUBLIC_APP_ENV=production`.
- Target domain (verify DNS before use): `invoices.fixna.in`.

## Verify

- `GET /api/v1/health/ready` → 200 `{status: UP}`.
- Register → login → upload a PDF → extract → confirm a low‑confidence field →
  approve → export CSV. Confirm the original is only reachable via the
  authenticated `/content` endpoint and that cross‑tenant access returns 404.
