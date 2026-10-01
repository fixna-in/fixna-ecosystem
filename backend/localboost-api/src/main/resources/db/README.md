# LocalBoost database (Flyway)

Migrations in `migration/` apply automatically on API startup.

**Database name (local):** `fixna`  
**Port:** 8080

## Migration index

| Version | File | Purpose |
|---------|------|---------|
| V1 | `V1__create_tenants_users.sql` | Tenants, users, memberships |
| V2 | `V2__create_businesses.sql` | Businesses |
| V3 | `V3__create_campaigns.sql` | Campaigns |
| V4 | `V4__create_targeting_creatives.sql` | Geo, audiences, creatives |
| V5 | `V5__create_platform_analytics_leads.sql` | Platform, analytics, leads |
| V6 | `V6__create_ai_billing_audit.sql` | Billing, audit |
| V7 | `V7__create_refresh_tokens.sql` | Refresh tokens |
| V8 | `V8__create_ai_usage_log.sql` | AI usage |
| V9 | `V9__plans_and_ai_quota.sql` | Plan quotas |

See [`docs/database/README.md`](../../../../../../docs/database/README.md).
