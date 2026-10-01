# Phase 3 — Project Management

**Status:** Complete  
**Date:** 2026-09-28  
**Service:** `backend/consulting-api`  
**Frontend:** `frontend/consulting-web`

## Scope delivered

| Capability | Status | Location |
|------------|--------|----------|
| Engagement | Done | `consulting/engagement/` |
| Project | Done | `consulting/project/` |
| Milestone | Done | `consulting/project/` |
| Task | Done | `consulting/project/` |
| Lifecycle transitions | Done | `*Status.java` enums with `validateTransitionTo` |
| Domain events | Done | `EngagementCreated`, `EngagementStatusChanged`, `ProjectCreated`, `ProjectStatusChanged` |
| Flyway V3 migration | Done | `V3__project_management.sql` |
| Frontend /projects | Done | `frontend/consulting-web/src/app/projects/` |

## Entity hierarchy

```
Client → Engagement → Project → Milestone
                            └→ Task (optional milestone link)
```

## Lifecycle states

| Entity | States | Terminal |
|--------|--------|----------|
| Engagement | DRAFT → ACTIVE ↔ ON_HOLD → COMPLETED / CANCELLED | COMPLETED, CANCELLED |
| Project | PLANNED → IN_PROGRESS ↔ ON_HOLD → COMPLETED / CANCELLED | COMPLETED, CANCELLED |
| Milestone | PENDING → IN_PROGRESS → COMPLETED / CANCELLED | COMPLETED, CANCELLED |
| Task | TODO → IN_PROGRESS ↔ BLOCKED → DONE / CANCELLED | DONE, CANCELLED |

Invalid transitions return **400** with code `INVALID_STATUS_TRANSITION`.

## API endpoints

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/v1/clients/{clientId}/engagements` | PROJECT_VIEW |
| POST | `/api/v1/clients/{clientId}/engagements` | PROJECT_MANAGE |
| GET | `/api/v1/engagements/{id}` | PROJECT_VIEW |
| PUT | `/api/v1/engagements/{id}` | PROJECT_MANAGE |
| POST | `/api/v1/engagements/{id}/transition` | PROJECT_MANAGE |
| GET | `/api/v1/projects` | PROJECT_VIEW |
| POST | `/api/v1/projects` | PROJECT_MANAGE |
| GET | `/api/v1/projects/{id}` | PROJECT_VIEW |
| PUT | `/api/v1/projects/{id}` | PROJECT_MANAGE |
| POST | `/api/v1/projects/{id}/transition` | PROJECT_MANAGE |
| GET | `/api/v1/projects/{id}/milestones` | PROJECT_VIEW |
| POST | `/api/v1/projects/{id}/milestones` | PROJECT_MANAGE |
| POST | `/api/v1/milestones/{id}/transition` | PROJECT_MANAGE |
| GET | `/api/v1/projects/{id}/tasks` | TASK_VIEW |
| POST | `/api/v1/projects/{id}/tasks` | TASK_MANAGE |
| PUT | `/api/v1/tasks/{id}` | TASK_MANAGE |
| POST | `/api/v1/tasks/{id}/transition` | TASK_MANAGE |

## Tenant isolation

All repository lookups use `findByIdAndTenantId` / tenant-scoped list queries. Cross-tenant access returns **404 NOT_FOUND**.

## Tests

- `EngagementApplicationServiceTest` — create, transition, RBAC, not-found
- `ProjectApplicationServiceTest` — create project, transition, invalid task transition, not-found
- `ProjectLifecycleTest` — enum transition rules for engagement, project, milestone, task

```
mvn -f backend/consulting-api/pom.xml test
BUILD SUCCESS — 35 tests, 0 failures, 1 skipped (Docker context load)
```

## Run locally

1. Ensure PostgreSQL database `consulting` exists and Phase 1–2 migrations applied
2. Restart API to apply `V3`:
   ```bash
   mvn -f backend/consulting-api/pom.xml spring-boot:run
   ```
3. Open `http://localhost:3000/projects` (create a client on `/clients` first)

## Next: Phase 5 — Proposals

See [phase-4-meetings-calendar.md](./phase-4-meetings-calendar.md) (complete). Next up: Proposal, ProposalItem, send/approve lifecycle.
