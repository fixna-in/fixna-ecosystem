# Deployment guides

Each Fixna product deploys as **two separate units** (API + web) with its own database and Flyway migrations.

| Product | API blueprint | Web (Vercel root) | Database docs | Checklist |
|---------|---------------|-------------------|---------------|-----------|
| LocalBoost | `render-localboost.yaml` | `frontend/localboost-web` | [database/README.md](../database/README.md) | [localboost.md](localboost.md) |
| Consulting | `render-consulting.yaml` | `frontend/consulting-web` | [database/README.md](../database/README.md) | [consulting.md](consulting.md) |
| Website Audit | `render-website-audit.yaml` | `frontend/website-audit-web` | [database/README.md](../database/README.md) | [website-audit.md](website-audit.md) |
| Invoice Assistant | `render-invoice-assistant.yaml` | `frontend/invoice-assistant-web` | [database/README.md](../database/README.md) | [invoice-assistant.md](invoice-assistant.md) |

**Database scripts live in each API service:**

```
backend/consulting-api/src/main/resources/db/migration/   # V1–V11
backend/localboost-api/src/main/resources/db/migration/   # V1–V9
backend/website-audit-api/src/main/resources/db/migration/ # V1–V3
backend/invoice-assistant-api/src/main/resources/db/migration/ # V1–V6
tools/localboost/sql/                                        # demo/reference SQL
```

Shared pattern: [scaffolding.md](../scaffolding.md)

Local dev infrastructure: `docker compose up -d` (repo root)
