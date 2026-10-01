# Database isolation

Each product has its **own PostgreSQL database** and **own Flyway migration directory**. Migrations must never be merged or shared between products.

## Databases

| Product | Database name | Native PostgreSQL port | Migrations |
|---------|---------------|------------|------------|
| LocalBoost | `localboost` | 5432 | `backend/localboost-api/src/main/resources/db/migration/` |
| Consulting | `consulting` | 5432 | `backend/consulting-api/src/main/resources/db/migration/` |

Each environment has its own PostgreSQL instance. Consulting uses a database named `consulting` in local, staging, and production; do not share one physical database across environments. Docker Compose remains an optional local setup and publishes its two PostgreSQL services on ports 5432 and 5433.

```bash
docker compose up -d
```

## Connection defaults

```bash
# LocalBoost native PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/localboost
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your-local-postgres-password

# Consulting native PostgreSQL (activate the local Spring profile)
SPRING_PROFILES_ACTIVE=local
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/consulting
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your-local-postgres-password

# Optional Compose endpoint
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/consulting
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
```

Flyway runs automatically on API startup (`spring.flyway.enabled: true`).

## Rules

1. **Never merge** `consulting-api` and `localboost-api` migration files.
2. **Never point** LocalBoost API at the Consulting database (or vice versa).
3. Production databases are provisioned per product in Render/Neon.
4. LocalBoost production currently uses database `fixna` on Neon — do not rename without a planned migration.

## Migration index

| Product | Versions | Docs |
|---------|----------|------|
| LocalBoost | V1–V9 | [`docs/database/README.md`](../database/README.md), [`docs/products/localboost/`](../products/localboost/) |
| Consulting | V1–V9 | [`docs/implementation/`](../implementation/) phase docs |

See [ADR-008](../decisions/ADR-008-database-isolation.md).
