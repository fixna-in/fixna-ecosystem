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

**Must not contain:** Consulting client/project/proposal/invoice domains; Website Audit domains; Invoice Assistant domains; Hospitality domains.

## Consulting

| Layer | Path | Planned URLs |
|-------|------|--------------|
| API | `backend/consulting-api` | https://api.consulting.fixna.in |
| Web | `frontend/consulting-web` | https://consulting.fixna.in |
| Deploy | `render-consulting.yaml` | Render (API) + Vercel (web) |
| Database | PostgreSQL `consulting` | Render Postgres |

**Owns:** clients, engagements, projects, tasks, milestones, meetings, proposals, invoices, client portal, public website, consulting AI assistant.

**Must not contain:** LocalBoost campaign/lead/advertising domains; Website Audit domains; Invoice Assistant domains; Hospitality domains.

## Website Audit

| Layer | Path | Production URLs |
|-------|------|-----------------|
| API | `backend/website-audit-api` | https://api.websiteaudit.fixna.in |
| Web | `frontend/website-audit-web` | https://websiteaudit.fixna.in |
| Deploy | `render-website-audit.yaml` | Render (API) + Vercel (web) |
| Database | PostgreSQL `website_audit` | Render Postgres |

**Owns:** website audits, crawl jobs, findings, checks, remediation plans, AI fix suggestions.

**Must not contain:** LocalBoost domains; Consulting domains; Invoice Assistant domains; Hospitality domains.

## Invoice Assistant

| Layer | Path | Production URLs |
|-------|------|-----------------|
| API | `backend/invoice-assistant-api` | https://api.invoice.fixna.in |
| Web | `frontend/invoice-assistant-web` | https://invoice.fixna.in |
| Deploy | `render-invoice-assistant.yaml` | Render (API) + Vercel (web) |
| Database | PostgreSQL `invoice_assistant` | Render Postgres |

**Owns:** documents, extraction jobs, fields, confidence scores, review workflows, exports.

**Must not contain:** LocalBoost domains; Consulting domains; Website Audit domains; Hospitality domains.

## Hospitality

| Layer | Path | Production URLs |
|-------|------|-----------------|
| API | `backend/hospitality-api` | https://api.hospitality.fixna.in |
| Web | `frontend/hospitality-web` | https://hospitality.fixna.in |
| Deploy | `render-hospitality.yaml` | Render (API) + Vercel (web) |
| Database | PostgreSQL `hospitality` | Render Postgres |

**Owns:** operations, guest experiences, service workflows, business configuration.

**Must not contain:** LocalBoost domains; Consulting domains; Website Audit domains; Invoice Assistant domains.

## Prohibited dependencies

```
LocalBoost API       ✗→  Consulting API
Consulting API       ✗→  LocalBoost API
Website Audit API    ✗→  LocalBoost API
Website Audit API    ✗→  Consulting API
Invoice Assistant    ✗→  LocalBoost API
Invoice Assistant    ✗→  Consulting API
Hospitality API      ✗→  LocalBoost API
Hospitality API      ✗→  Consulting API
platform-common      ✗→  any product package
```

Allowed:

```
LocalBoost API       →  fixna-platform-common
Consulting API       →  fixna-platform-common
Website Audit API    →  fixna-platform-common
Invoice Assistant    →  fixna-platform-common
Hospitality API      →  fixna-platform-common
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
| Website Audit | Tenant → Audit Jobs → Quota limits |
| Invoice Assistant | Tenant → Documents → Quota limits |
| Hospitality | Tenant → Subscription → Plan → usage limits |

Shared primitives (`PaymentProvider`, money types) may exist per product; subscription and invoice lifecycles stay separate.

See [ADR-006](../decisions/ADR-006-consulting-billing-boundary.md).
