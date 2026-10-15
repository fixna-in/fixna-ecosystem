# Flyway migrations — Hospitality API

Each migration is a versioned SQL file (`V{number}__{description}.sql`).

- Never modify an applied migration. Create a new one instead.
- Run locally: `mvn -f backend/hospitality-api/pom.xml flyway:migrate`
- Prod: migrations run automatically on container start.

Baseline: none — V1 creates core tables.