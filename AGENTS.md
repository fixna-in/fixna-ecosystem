# Fixna Ecosystem — Agent & development guide

This monorepo hosts **multiple independent products**. Each product deploys separately (API + web + database).

## Products

| Product | API | Web | Agent rules | Requirements |
|---------|-----|-----|-------------|--------------|
| **LocalBoost** | `backend/localboost-api` | `frontend/localboost-web` | `.cursor/products/localboost/` | `docs/products/localboost/requirements/` |
| **Consulting** | `backend/consulting-api` | `frontend/consulting-web` | `.cursor/products/consulting/` | `docs/implementation/` |
| **Hospitality** | _(future)_ | _(future)_ | — | — |

## Before every change

1. Read this file.
2. Read the product-specific rules under `.cursor/products/{product}/rules/`.
3. Read relevant requirements and architecture docs for that product only.
4. Inspect existing code before creating or replacing files.
5. Check database migrations and API contracts for **that product**.
6. Implement only the requested scope — do not merge domains across products.
7. Add or update tests.
8. Update documentation when behavior or contracts change.

## Non-negotiable (all products)

- Preserve multi-tenant isolation.
- Never trust a tenant ID supplied by the client.
- Never expose secrets or tokens in logs.
- Every schema change requires a **new Flyway migration** in that product's service only.
- AI output is untrusted — validate before use; no autonomous spending or external actions.
- Controllers contain no business logic.
- Do not introduce dependencies without a documented reason.
- Do not rewrite unrelated code.
- **Never merge Flyway migrations** between `consulting-api`, `localboost-api`, or future services.

## Database scripts

| Product | Migrations | Docs |
|---------|------------|------|
| Consulting | `backend/consulting-api/src/main/resources/db/migration/` | `backend/consulting-api/src/main/resources/db/README.md` |
| LocalBoost | `backend/localboost-api/src/main/resources/db/migration/` | `backend/localboost-api/src/main/resources/db/README.md` |

Index: [`docs/database/README.md`](docs/database/README.md)

## Local development

```bash
docker compose up -d                    # Postgres (both products) + Redis + Mailhog
npm run test:all-apis                   # All API tests
npm run build:all                       # All web production builds
```

Per product — see [`docs/scaffolding.md`](docs/scaffolding.md) and service READMEs.

## LocalBoost legacy content

Migrated from standalone `fixna-localboost` repo (retiring):

- Product docs: `docs/products/localboost/`
- AI prompts/schemas: `ai/localboost/`
- Cursor workflows: `.cursor/products/localboost/workflows/`
- Demo SQL: `tools/localboost/sql/`
- Original agent guide: `docs/products/localboost/AGENTS.md`

## Completion report

At the end of a task report:

- files changed
- behavior implemented
- tests added/run
- migrations added (if any)
- assumptions
- known limitations
- next recommended task
