# Database scripts — Fixna Ecosystem

Each product has its **own PostgreSQL database** and **own Flyway migration history**. Never merge migrations across services.

## Quick reference

| Product | Flyway migrations | Local seed | Reference / demo SQL |
|---------|-------------------|------------|----------------------|
| **Consulting** | [`backend/consulting-api/src/main/resources/db/migration/`](../../backend/consulting-api/src/main/resources/db/migration/) | [`db/seed/local-test-data.sql`](../../backend/consulting-api/src/main/resources/db/seed/local-test-data.sql) | [`tools/consulting/sql/`](../../tools/consulting/sql/) |
| **LocalBoost** | [`backend/localboost-api/src/main/resources/db/migration/`](../../backend/localboost-api/src/main/resources/db/migration/) | [`db/seed/local-test-data.sql`](../../backend/localboost-api/src/main/resources/db/seed/local-test-data.sql) | [`tools/localboost/sql/`](../../tools/localboost/sql/) |

## How migrations run

Flyway runs **automatically on API startup** (`spring.flyway.enabled: true` in each service).

```bash
# Consulting — native PostgreSQL on localhost:5432, database consulting
SPRING_PROFILES_ACTIVE="local" POSTGRES_USER="postgres" POSTGRES_PASSWORD="admin" mvn -f backend/consulting-api/pom.xml spring-boot:run

# LocalBoost (port 8080, database: localboost)
mvn -f backend/localboost-api/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
```

The local profiles use native PostgreSQL on `localhost:5432`. Create separate
`consulting` and `localboost` databases in that PostgreSQL instance. Set
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
| Redis (LocalBoost) | 6379 | — | — |

Connection URLs:

```
# Consulting Compose database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/consulting

# LocalBoost
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fixna
```

## Product documentation

- Consulting schema phases: [`docs/implementation/`](../implementation/) (V1–V9)
- LocalBoost schema docs: [`docs/products/localboost/04-database/`](../products/localboost/04-database/)
- LocalBoost requirements: [`docs/products/localboost/requirements/`](../products/localboost/requirements/)

## Manual demo seed (LocalBoost / Neon)

For shared demo environments, load reference SQL manually:

```bash
# From repo root — adjust connection for your Neon branch
psql "$SPRING_DATASOURCE_URL" -f tools/localboost/sql/neon-demo-seed.sql
```

See also `tools/localboost/sql/demo-data.sql` and `tools/localboost/sql/schema.sql` for standalone reference.
