# Phase 4 — Meetings & Calendar

**Status:** Complete  
**Date:** 2026-09-28  
**Service:** `backend/consulting-api`  
**Frontend:** `frontend/consulting-web`

## Scope delivered

| Capability | Status | Location |
|------------|--------|----------|
| Meeting | Done | `consulting/meeting/` |
| CalendarProvider abstraction | Done | `scheduling/CalendarProvider.java` |
| InternalCalendarProvider | Done | `scheduling/InternalCalendarProvider.java` |
| Conflict detection | Done | Organizer overlap check in internal provider |
| Lifecycle transitions | Done | `MeetingStatus.java` |
| Domain events | Done | `MeetingScheduled`, `MeetingStatusChanged` |
| Flyway V4 migration | Done | `V4__meetings.sql` |
| Frontend /meetings | Done | `frontend/consulting-web/src/app/meetings/` |

## Entity links

```
Client → Meeting (optional Engagement, optional Project)
Meeting → calendar_provider + calendar_event_id (internal first)
```

## Lifecycle states

| Status | Transitions |
|--------|-------------|
| SCHEDULED | → IN_PROGRESS, CANCELLED |
| IN_PROGRESS | → COMPLETED, CANCELLED |
| COMPLETED | terminal |
| CANCELLED | terminal |

## API endpoints

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/v1/meetings` | MEETING_VIEW |
| POST | `/api/v1/meetings` | MEETING_MANAGE |
| GET | `/api/v1/meetings/{id}` | MEETING_VIEW |
| PUT | `/api/v1/meetings/{id}` | MEETING_MANAGE |
| POST | `/api/v1/meetings/{id}/transition` | MEETING_MANAGE |

Query params for list: `clientId`, `projectId`

## Calendar provider

- `CalendarProvider` — schedule, reschedule, cancel
- `InternalCalendarProvider` — validates time range, detects organizer conflicts (`CALENDAR_CONFLICT`)
- Future: Google/Microsoft providers plug in via same interface

## Tests

- `MeetingApplicationServiceTest` — schedule, transition, RBAC, not-found
- `InternalCalendarProviderTest` — conflict detection, invalid time range
- `MeetingLifecycleTest` — enum transition rules

## Next: Phase 6 — Invoicing

See [phase-5-proposals.md](./phase-5-proposals.md) (complete). Next up: Invoice, InvoiceLine, Payment.
