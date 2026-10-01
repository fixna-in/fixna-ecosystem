# Phase 2 — Consulting CRM

**Status:** Complete  
**Date:** 2026-09-28  
**Service:** `backend/consulting-api`  
**Frontend:** `frontend/consulting-web`

## Scope delivered

| Capability | Status | Location |
|------------|--------|----------|
| Client | Done | `consulting/client/` |
| ClientContact | Done | `consulting/client/` |
| ClientPortalMembership | Done | `consulting/client/` |
| Consulting Profile | Done | `consulting/profile/` |
| Service catalog | Done | `consulting/service/` |
| Domain events | Done | `ClientCreated`, `ClientUpdated` |
| Flyway V2 migration | Done | `V2__consulting_crm.sql` |
| Frontend /clients | Done | `frontend/consulting-web/src/app/clients/` |

## API endpoints

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/v1/clients` | CLIENT_VIEW |
| POST | `/api/v1/clients` | CLIENT_MANAGE |
| GET | `/api/v1/clients/{id}` | CLIENT_VIEW |
| PUT | `/api/v1/clients/{id}` | CLIENT_MANAGE |
| GET | `/api/v1/clients/{id}/contacts` | CLIENT_VIEW |
| POST | `/api/v1/clients/{id}/contacts` | CLIENT_MANAGE |
| GET | `/api/v1/clients/{id}/portal-memberships` | CLIENT_VIEW |
| POST | `/api/v1/clients/{id}/portal-memberships` | CLIENT_MANAGE |
| GET | `/api/v1/profile` | CLIENT_VIEW |
| PUT | `/api/v1/profile` | WEBSITE_MANAGE |
| GET | `/api/v1/services` | CLIENT_VIEW |
| POST | `/api/v1/services` | CLIENT_MANAGE |
| GET | `/api/v1/services/{id}` | CLIENT_VIEW |
| PUT | `/api/v1/services/{id}` | CLIENT_MANAGE |

## Tenant isolation

All repository lookups use `findByIdAndTenantId` / `findByTenantId`. Cross-tenant access returns **404 NOT_FOUND**.

## Tests

- `ClientApplicationServiceTest` — create, update, RBAC, not-found
- `ClientTenantIsolationTest` — cross-tenant repository isolation

## Next: Phase 4 — Meetings & Calendar

See [phase-3-project-management.md](./phase-3-project-management.md) (complete). Next up: Meeting entity and CalendarProvider abstraction.
