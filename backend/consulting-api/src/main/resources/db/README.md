# Consulting database (Flyway)

Migrations in `migration/` apply automatically on API startup.

**Database name:** `consulting`  
**Port:** 5432 (native PostgreSQL), API **8081**

Create the `consulting` database in the PostgreSQL instance for this environment
if it does not already exist:

```sql
CREATE DATABASE consulting;
```

Then start the API as a Spring Boot application from the repository root:

```bash
SPRING_PROFILES_ACTIVE="local" POSTGRES_USER="postgres" POSTGRES_PASSWORD="admin" mvn -f backend/consulting-api/pom.xml spring-boot:run
```

PowerShell equivalent:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:POSTGRES_USER = "postgres"
$env:POSTGRES_PASSWORD = "admin"
mvn -f backend/consulting-api/pom.xml spring-boot:run
```

For an empty database, Flyway applies the Consulting migrations on startup.
The `local` profile can also adopt an existing non-empty schema at baseline
version 9, which is intended only for a schema created from the checked-in
V1-V9 `tools/consulting/sql/schema.sql` snapshot. Hibernate validates mapped
tables at startup; do not use this to adopt an unknown or partial schema.
The `local` profile uses
`localhost:5432/consulting`; override `SPRING_DATASOURCE_URL` if your
database uses a different host, port, or database name. No Docker container is
needed to run the API.
The default local CORS allowlist permits the Consulting web app on ports 3001
and 3002 (`localhost` and `127.0.0.1`). Restart the API after changing this
configuration; override `FIXNA_CORS_ALLOWED_ORIGINS` to use another web origin.

Use a separate PostgreSQL instance for each environment, with the database
named `consulting` in each. Do not point local, staging, and production at one
shared database. If an existing database has tables but no
`flyway_schema_history`, do not enable automatic baselining until its schema
has been checked against the Consulting migrations.

## Migration index

| Version | File | Purpose |
|---------|------|---------|
| V1 | `V1__platform_core.sql` | Tenants, users, auth, audit, notifications |
| V2 | `V2__consulting_crm.sql` | Clients, contacts, portal memberships, profile, services |
| V3 | `V3__project_management.sql` | Engagements, projects, milestones, tasks |
| V4 | `V4__meetings.sql` | Meetings |
| V5 | `V5__proposals.sql` | Proposals and line items |
| V6 | `V6__invoicing.sql` | Invoices, lines, payments |
| V7 | `V7__client_portal.sql` | Portal refresh token scope |
| V8 | `V8__public_website.sql` | Public slug, testimonials |
| V9 | `V9__ai_assistance.sql` | AI usage logs |

## Local seed data

| Script | Purpose |
|--------|---------|
| [`seed/local-test-data.sql`](seed/local-test-data.sql) | JDBC fixture loaded by `LocalTestDataSeeder` (profile `local`) |
| [`tools/consulting/sql/demo-data.sql`](../../../../../../tools/consulting/sql/demo-data.sql) | Manual psql demo load |
| [`tools/consulting/sql/schema.sql`](../../../../../../tools/consulting/sql/schema.sql) | Reference DDL snapshot (empty DB only) |

Enable API seeder:

```bash
export SPRING_PROFILES_ACTIVE=local
export FIXNA_TEST_DATA_ENABLED=true
export FIXNA_TEST_USER_PASSWORD=your-local-password
```

See [`docs/database/README.md`](../../../../../../docs/database/README.md).
