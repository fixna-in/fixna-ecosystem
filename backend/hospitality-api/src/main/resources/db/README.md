# Flyway migrations — Hospitality API

Each migration is a versioned SQL file (`V{number}__{description}.sql`).

- Never modify an applied migration. Create a new one instead.
- Run locally: `mvn -f backend/hospitality-api/pom.xml flyway:migrate`
- Prod: migrations run automatically on container start.

Baseline: none — V1 creates core tables.

## Migrations

| Version | File | Purpose |
|---------|------|---------|
| V1 | `V1__platform_core.sql` | tenants, users, tenant_memberships, refresh_tokens, audit_logs, notifications |
| V2 | `V2__subscriptions.sql` | Subscriptions (shared billing entity from `fixna-platform-common`, 1:1 per tenant) |
| V3 | `V3__webhook_events.sql` | Idempotent billing webhook events (shared billing entity) |

V2/V3 exist because the shared `Subscription`/`WebhookEvent` entities sit on the
hospitality component-scan path (`in.fixna.platform.billing`) and are validated by
Hibernate at startup (`ddl-auto: validate`).