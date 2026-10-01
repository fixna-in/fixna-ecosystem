# Deployment guides

Each Fixna product deploys as **two separate units** (API + web) with its own database and Flyway migrations.

| Product | API blueprint | Web (Vercel root) | Database docs | Checklist |
|---------|---------------|-------------------|---------------|-----------|
| LocalBoost | `render-localboost.yaml` | `frontend/localboost-web` | [database/README.md](../database/README.md) | [localboost.md](localboost.md) |
| Consulting | `render-consulting.yaml` | `frontend/consulting-web` | [database/README.md](../database/README.md) | [consulting.md](consulting.md) |

**Database scripts live in each API service:**

```
backend/consulting-api/src/main/resources/db/migration/   # V1–V9
backend/localboost-api/src/main/resources/db/migration/   # V1–V9
tools/localboost/sql/                                        # demo/reference SQL
```

Shared pattern: [scaffolding.md](../scaffolding.md)

Local dev infrastructure: `docker compose up -d` (repo root)
