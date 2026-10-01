# Deployment architecture

Each product deploys as **API + web + database** — no shared runtime.

## Overview

| Product | API host | Web host | Blueprint |
|---------|----------|----------|-----------|
| LocalBoost | Render (Docker) | Vercel | `render-localboost.yaml` |
| Consulting | Render (Docker) | Vercel | `render-consulting.yaml` |

Production LocalBoost still runs from standalone `fixna-localboost` until ecosystem cutover; monorepo configs are ready.

## API (Render)

Both APIs use **monorepo Docker builds** with `dockerContext: .` at repo root so `fixna-platform-common` is included.

| Product | Dockerfile | Health check |
|---------|------------|--------------|
| LocalBoost | `backend/localboost-api/Dockerfile` | `/api/v1/health/readiness` |
| Consulting | `backend/consulting-api/Dockerfile` | `/api/v1/health/ready` |

Build locally:

```bash
# LocalBoost
docker build -f backend/localboost-api/Dockerfile -t fixna-localboost-api .

# Consulting
docker build -f backend/consulting-api/Dockerfile -t fixna-consulting-api .
```

Required secrets (set in Render, never committed): `FIXNA_JWT_SECRET`, `FIXNA_CORS_ALLOWED_ORIGINS`, database credentials.

## Web (Vercel)

| Product | Root directory | API env var |
|---------|----------------|-------------|
| LocalBoost | `frontend/localboost-web` | `NEXT_PUBLIC_API_BASE_URL=https://api.fixna.in/api` |
| Consulting | `frontend/consulting-web` | `NEXT_PUBLIC_API_BASE_URL=https://api.consulting.fixna.in/api` |

Each frontend calls **only its own backend** — no cross-product API URLs.

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs independent jobs per product:

1. Bootstrap parent POM + `fixna-platform-common`
2. Test/package each API separately
3. Build each web app separately

## Checklists

- [LocalBoost deployment](../deployment/localboost.md)
- [Consulting deployment](../deployment/consulting.md)
