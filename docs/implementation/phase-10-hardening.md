# Phase 10 — Hardening & Deployment

**Status:** Complete  
**Version:** 1.0.0 (Consulting MVP)  
**Date:** 2026-09-28

## Goal

Make the Consulting MVP production-ready: security validation, deployment artifacts, and CI quality gates.

## Backend hardening

### Production profile (`application-prod.yml`)
- HSTS enabled
- Swagger/OpenAPI disabled
- Actuator limited to `health` + `info`; health details hidden
- Error responses omit stack traces and binding details
- Production logging levels (WARN root, INFO platform)

### Startup validation (`ProductionSecurityValidator`)
Active on `prod` profile only:
- Rejects default/dev JWT secrets
- Requires JWT secret length ≥ 32
- Rejects localhost in CORS origins

### Security headers
- Added `Permissions-Policy` on all responses

## Deployment

| Artifact | Purpose |
|----------|---------|
| `backend/consulting-api/Dockerfile` | Multi-stage JRE 21 container with readiness healthcheck |
| `render-consulting.yaml` | Render blueprint for API + PostgreSQL |

Suggested domains (from `docs/build-and-deployment.md`):
- API: Render → `api.consulting.fixna.in`
- Web: Vercel → `consulting.fixna.in`

### Required production env vars
- `SPRING_PROFILES_ACTIVE=prod`
- `FIXNA_JWT_SECRET` (≥ 32 chars, not a dev default)
- `FIXNA_CORS_ALLOWED_ORIGINS` (production domains only)
- `SPRING_DATASOURCE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`

## CI (`.github/workflows/ci.yml`)

| Job | Gate |
|-----|------|
| `consulting-api` | `mvn test`, `mvn package`, migration count ≥ 9 |
| `consulting-web` | `npm ci`, `npm run build:consulting` |

## Tests

- `ProductionSecurityValidatorTest` — secret and CORS rules
- `DeploymentReadinessTest` — migrations, prod config, Docker/Render/CI artifacts
- `PublicEndpointComplianceTest` — JWT filter bypass list

## MVP complete

Phases 1–10 delivered. Next work is post-MVP: real AI provider, payment gateways, calendar integrations, and hospitality track.
