# Product boundaries

Each product is a separate deployable unit with its own backend, frontend, database, and migration history.

## LocalBoost

| Layer | Path | Production URLs |
|-------|------|-----------------|
| API | `backend/localboost-api` | https://api.fixna.in |
| Web | `frontend/localboost-web` | https://app.fixna.in |
| Deploy | `render-localboost.yaml` | Render (API) + Vercel (web) |
| Database | PostgreSQL `fixna` | Neon / Render Postgres |

**Owns:** campaigns, businesses, leads, audiences, creatives, platform adapters, subscription billing, LocalBoost AI recommendations.

**Must not contain:** Consulting client/project/proposal/invoice domains.

## Consulting

| Layer | Path | Planned URLs |
|-------|------|--------------|
| API | `backend/consulting-api` | https://api.consulting.fixna.in |
| Web | `frontend/consulting-web` | https://consulting.fixna.in |
| Deploy | `render-consulting.yaml` | Render (API) + Vercel (web) |
| Database | PostgreSQL `consulting` | Render Postgres |

**Owns:** clients, engagements, projects, tasks, milestones, meetings, proposals, invoices, client portal, public website, consulting AI assistant.

**Must not contain:** LocalBoost campaign/lead/advertising domains.

## Hospitality (future)

| Layer | Path |
|-------|------|
| API | `backend/hospitality-api` (README placeholder) |
| Web | `frontend/hospitality-web` (README placeholder) |

No implementation until explicitly scoped. Must not create dependencies from LocalBoost or Consulting.

## Prohibited dependencies

```
LocalBoost API  ✗→  Consulting API
Consulting API  ✗→  LocalBoost API
platform-common ✗→  any product package
```

Allowed:

```
LocalBoost API  →  fixna-platform-common
Consulting API  →  fixna-platform-common
```

## User vs client (Consulting)

Platform **Users** (consultants, admins) authenticate via `/api/v1/auth`.

**Clients** are business entities. Client portal users link via `ClientPortalMembership`:

```
User → ClientPortalMembership → Client
```

Portal JWT includes a `clientId` claim; tenant scope still comes from authenticated context.

## Billing separation

| Product | Model |
|---------|-------|
| LocalBoost | Tenant → Subscription → Plan → usage limits |
| Consulting | Client → Engagement → Invoice → InvoiceLine → Payment |

Shared primitives (`PaymentProvider`, money types) may exist per product; subscription and invoice lifecycles stay separate.

See [ADR-006](../decisions/ADR-006-consulting-billing-boundary.md).
