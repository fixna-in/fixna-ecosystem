# Database scripts — Fixna Ecosystem

Each product has its **own PostgreSQL database** and **own Flyway migration history**. Never merge migrations across services.

## Quick reference

| Product | Flyway migrations | Local seed | Reference / demo SQL |
|---------|-------------------|------------|----------------------|
| **Consulting** | [`backend/consulting-api/src/main/resources/db/migration/`](../../backend/consulting-api/src/main/resources/db/migration/) | [`db/seed/local-test-data.sql`](../../backend/consulting-api/src/main/resources/db/seed/local-test-data.sql) | [`tools/consulting/sql/`](../../tools/consulting/sql/) |
| **LocalBoost** | [`backend/localboost-api/src/main/resources/db/migration/`](../../backend/localboost-api/src/main/resources/db/migration/) | [`db/seed/local-test-data.sql`](../../backend/localboost-api/src/main/resources/db/seed/local-test-data.sql) | [`tools/localboost/sql/`](../../tools/localboost/sql/) |
| **Website Audit** | [`backend/website-audit-api/src/main/resources/db/migration/`](../../backend/website-audit-api/src/main/resources/db/migration/) | — | — |
| **Invoice Assistant** | [`backend/invoice-assistant-api/src/main/resources/db/migration/`](../../backend/invoice-assistant-api/src/main/resources/db/migration/) | — | — |
| **Hospitality** | [`backend/hospitality-api/src/main/resources/db/migration/`](../../backend/hospitality-api/src/main/resources/db/migration/) | — | — |

## How migrations run

Flyway runs **automatically on API startup** (`spring.flyway.enabled: true` in each service).

```bash
# Consulting — native PostgreSQL on localhost:5432, database consulting
SPRING_PROFILES_ACTIVE="local" POSTGRES_USER="postgres" POSTGRES_PASSWORD="admin" mvn -f backend/consulting-api/pom.xml spring-boot:run

# LocalBoost (port 8080, database: localboost)
mvn -f backend/localboost-api/pom.xml spring-boot:run -Dspring-boot.run.profiles=local

# Website Audit (port 8082, database: website_audit)
mvn -f backend/website-audit-api/pom.xml spring-boot:run -Dspring-boot.run.profiles=local

# Invoice Assistant (port 8083, database: invoice_assistant)
mvn -f backend/invoice-assistant-api/pom.xml spring-boot:run -Dspring-boot.run.profiles=local

# Hospitality (port 8084, database: hospitality)
mvn -f backend/hospitality-api/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
```

The local profiles use native PostgreSQL on `localhost:5432`. Create separate
databases for each product in that PostgreSQL instance. Set
`POSTGRES_PASSWORD` to the password configured for your local PostgreSQL user
before starting each API. Each environment must use its own database server;
the Consulting database is named `consulting` in each environment. Consulting
accepts `POSTGRES_USER` (default `postgres`); LocalBoost uses `postgres`.

## Optional Docker PostgreSQL

Compose is an alternative when you want containerized databases:

```bash
docker compose -f docker-compose.yml up -d
```

| Service | Host port | Database | Default user/password |
|---------|-----------|----------|------------------------|
| Consulting Postgres | 5433 | `consulting` | `postgres` / `change-me` |
| LocalBoost Postgres | 5432 | `fixna` | `fixna` / `change-me` |
| Website Audit Postgres | 5434 | `website_audit` | `postgres` / `change-me` |
| Invoice Assistant Postgres | 5435 | `invoice_assistant` | `postgres` / `change-me` |
| Hospitality Postgres | 5436 | `hospitality` | `postgres` / `change-me` |
| Redis (LocalBoost) | 6379 | — | — |

Connection URLs:

```
# Consulting Compose database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/consulting

# LocalBoost
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fixna

# Website Audit
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/website_audit

# Invoice Assistant
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5435/invoice_assistant

# Hospitality
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5436/hospitality
```

## Product documentation

- Consulting schema phases: [`docs/implementation/`](../implementation/) (V1–V11)
- LocalBoost schema docs: [`docs/products/localboost/04-database/`](../products/localboost/04-database/)
- LocalBoost requirements: [`docs/products/localboost/requirements/`](../products/localboost/requirements/)
- Website Audit specs: [`docs/specs/website-audit-database-design.md`](../specs/website-audit-database-design.md)
- Invoice Assistant specs: [`docs/specs/invoice-assistant-database-design.md`](../specs/invoice-assistant-database-design.md)

## Manual demo seed (LocalBoost / Neon)

For shared demo environments, load reference SQL manually:

```bash
# From repo root — adjust connection for your Neon branch
psql "$SPRING_DATASOURCE_URL" -f tools/localboost/sql/neon-demo-seed.sql
```

See also `tools/localboost/sql/demo-data.sql` and `tools/localboost/sql/schema.sql` for standalone reference.
