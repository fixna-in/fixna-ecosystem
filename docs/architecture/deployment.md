# Deployment architecture

Each product deploys as **API + web + database** — no shared runtime.

## Overview

| Product | API host | Web host | Blueprint |
|---------|----------|----------|-----------|
| LocalBoost | Render (Docker) | Vercel | `render-localboost.yaml` |
| Consulting | Render (Docker) | Vercel | `render-consulting.yaml` |
| Website Audit | Render (Docker) | Vercel | `render-website-audit.yaml` |
| Invoice Assistant | Render (Docker) | Vercel | `render-invoice-assistant.yaml` |
| Hospitality | Render (Docker) | Vercel | `render-hospitality.yaml` |

Production LocalBoost still runs from standalone `fixna-localboost` until ecosystem cutover; monorepo configs are ready.

## API (Render)

All APIs use **monorepo Docker builds** with `dockerContext: .` at repo root so `fixna-platform-common` is included.

| Product | Dockerfile | Health check |
|---------|------------|--------------|
| LocalBoost | `backend/localboost-api/Dockerfile` | `/api/v1/health/readiness` |
| Consulting | `backend/consulting-api/Dockerfile` | `/api/v1/health/ready` |
| Website Audit | `backend/website-audit-api/Dockerfile` | `/api/v1/health` |
| Invoice Assistant | `backend/invoice-assistant-api/Dockerfile` | `/api/v1/health` |
| Hospitality | `backend/hospitality-api/Dockerfile` | `/api/v1/health` |

Build locally:

```bash
# LocalBoost
docker build -f backend/localboost-api/Dockerfile -t fixna-localboost-api .

# Consulting
docker build -f backend/consulting-api/Dockerfile -t fixna-consulting-api .

# Website Audit
docker build -f backend/website-audit-api/Dockerfile -t fixna-website-audit-api .

# Invoice Assistant
docker build -f backend/invoice-assistant-api/Dockerfile -t fixna-invoice-assistant-api .

# Hospitality
docker build -f backend/hospitality-api/Dockerfile -t fixna-hospitality-api .
```

Required secrets (set in Render, never committed): `FIXNA_JWT_SECRET`, `FIXNA_CORS_ALLOWED_ORIGINS`, database credentials.

## Web (Vercel)

| Product | Root directory | API env var |
|---------|----------------|-------------|
| LocalBoost | `frontend/localboost-web` | `NEXT_PUBLIC_API_BASE_URL=https://api.fixna.in/api` |
| Consulting | `frontend/consulting-web` | `NEXT_PUBLIC_API_BASE_URL=https://api.consulting.fixna.in/api` |
| Website Audit | `frontend/website-audit-web` | `NEXT_PUBLIC_API_BASE_URL=https://api.websiteaudit.fixna.in/api` |
| Invoice Assistant | `frontend/invoice-assistant-web` | `NEXT_PUBLIC_API_BASE_URL=https://api.invoice.fixna.in/api` |
| Hospitality | `frontend/hospitality-web` | `NEXT_PUBLIC_API_BASE_URL=https://api.hospitality.fixna.in/api` |

Each frontend calls **only its own backend** — no cross-product API URLs.

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs independent jobs per product:

1. Bootstrap parent POM + `fixna-platform-common`
2. Test/package each API separately
3. Build each web app separately

## Checklists

- [LocalBoost deployment](../deployment/localboost.md)
- [Consulting deployment](../deployment/consulting.md)
- [Website Audit deployment](../deployment/website-audit.md)
- [Invoice Assistant deployment](../deployment/invoice-assistant.md)
- [Hospitality deployment](../deployment/hospitality.md)
