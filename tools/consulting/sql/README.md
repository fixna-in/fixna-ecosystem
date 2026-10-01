# Consulting database scripts

Reference and demo SQL for the **consulting** PostgreSQL database (native PostgreSQL on port **5432** locally).

| File | Purpose |
|------|---------|
| [`schema.sql`](schema.sql) | Full V1–V9 DDL snapshot for an **empty** local database only |
| [`demo-data.sql`](demo-data.sql) | Demo tenant + CRM/project/billing fixture (requires migrations) |
| [`repair-orphaned-user.sql`](repair-orphaned-user.sql) | Local-only repair for an existing account that has no tenant membership |

Flyway migrations (source of truth) live in:

`backend/consulting-api/src/main/resources/db/migration/`

## Recommended local workflow

1. Start the local PostgreSQL service and ensure the `consulting` database exists.
2. Start the API once as a Spring Boot process (applies Flyway automatically):

   ```bash
   npm run bootstrap:maven
   SPRING_PROFILES_ACTIVE="local" POSTGRES_USER="postgres" POSTGRES_PASSWORD="admin" mvn -f backend/consulting-api/pom.xml spring-boot:run
   ```

3. Optional demo data via API register, or load fixture:

   ```bash
   # Generate BCrypt hash (see tools/localboost/PASSWORD-UTILITY.md)
   FIXNA_TEST_PASSWORD_HASH=<bcrypt-hash> psql -h localhost -p 5432 -U postgres -d consulting -f tools/consulting/sql/demo-data.sql
   ```

If login returns `NO_MEMBERSHIP`, the account exists but is not attached to a
Consulting workspace. For a local account only, create a workspace and attach
that user as its administrator (replace the example email and workspace name):

```bash
psql -h localhost -p 5432 -U postgres -d consulting \
   -v user_email="owner@example.com" \
   -v workspace_name="My Consulting Workspace" \
   -f tools/consulting/sql/repair-orphaned-user.sql
```

The script refuses non-loopback connections, non-`consulting` databases,
unknown email addresses, and users who already have any tenant membership. It
does not change the user's password. Retry login with the existing password.

## Local test fixture (API seeder)

With Spring profile `local` and `fixna.test-data.enabled=true`, the API loads
`backend/consulting-api/src/main/resources/db/seed/local-test-data.sql` on startup.

Demo accounts (when seeded):

| Email | Role |
|-------|------|
| `consultant@example.com` | CONSULTANT_ADMIN |
| `client@example.com` | CLIENT_USER (portal) |

Set `FIXNA_TEST_USER_PASSWORD` (plaintext, local only) — never commit it.

See [`docs/database/README.md`](../../../docs/database/README.md).
