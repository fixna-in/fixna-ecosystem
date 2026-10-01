# ADR-008: Database Isolation

## Status
Accepted (2026-09-28)

## Context
LocalBoost and Consulting are separate products with separate deployment lifecycles. Sharing a database or Flyway history would create coupling and migration conflicts.

## Decision

1. Each product owns a dedicated PostgreSQL **database**:
   - LocalBoost: `fixna`
   - Consulting: `consulting`
2. Each product owns its Flyway migrations under `backend/{product}-api/src/main/resources/db/migration/`.
3. Local development may use one Docker Compose stack with **two Postgres services** (ports 5432 and 5433).
4. Migrations must **never** be copied or merged between products.
5. LocalBoost production database name `fixna` is preserved for Neon/Render compatibility.

## Consequences

- Connection strings and credentials are product-specific env vars.
- Schema changes require a new Flyway version in that product only.
- Cross-product reporting requires explicit integration (not shared tables).

See [database-isolation.md](../architecture/database-isolation.md).
