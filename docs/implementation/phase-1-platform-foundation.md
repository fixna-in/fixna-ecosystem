# Phase 1 — Platform Foundation

**Status:** Complete  
**Date:** 2026-09-28  
**Service:** `backend/consulting-api`

## Scope delivered

| Capability | Status | Location |
|------------|--------|----------|
| Identity (User) | Done | `identity/` |
| Authentication (JWT) | Done | `identity/auth/` |
| Tenancy (Tenant, TenantContext) | Done | `tenancy/` |
| Membership | Done | `membership/` |
| Permission-based RBAC | Done | `rbac/` |
| Audit | Done | `audit/` |
| Notification abstraction | Done | `notification/` (logging provider) |
| File storage abstraction | Done | `file/` (local provider) |
| Domain events | Done | `event/` (in-process) |
| Error handling | Done | `common/web/` |
| Observability | Done | `common/observability/`, health endpoints |
| Flyway migration V1 | Done | `db/migration/V1__platform_core.sql` |

## API endpoints

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/auth/refresh` | Public |
| POST | `/api/v1/auth/logout` | Authenticated |
| GET | `/api/v1/health` | Public |
| GET | `/api/v1/health/ready` | Public |

## RBAC roles

`PLATFORM_ADMIN`, `CONSULTANT_ADMIN`, `CONSULTANT`, `INTERNAL_TEAM`, `CLIENT_ADMIN`, `CLIENT_USER`

Registration creates `CONSULTING_WORKSPACE` tenant with `CONSULTANT_ADMIN` role.

## Tests

```
mvn -f backend/consulting-api/pom.xml test
BUILD SUCCESS — 18 tests, 0 failures, 1 skipped (Docker context load)
```

## Run locally

1. Create PostgreSQL database `consulting`
2. Set `POSTGRES_PASSWORD` and run:
   ```bash
   mvn -f backend/consulting-api/pom.xml spring-boot:run
   ```
3. API available at `http://localhost:8081`

## Limitations

- No rate limiting (deferred)
- Notification persistence not wired (table exists, logging provider only)
- No file upload HTTP endpoint yet
- `ApplicationContextLoadTest` requires Docker + Testcontainers
- No frontend for Consulting yet

## Next: Phase 2 — Consulting CRM

Client, ClientContact, ClientPortalMembership, Consulting Profile, Services.
