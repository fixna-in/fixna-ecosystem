# Cursor agent configuration — Fixna Ecosystem

Product-specific rules, workflows, and task tracking live under `.cursor/products/`.

## Index

| Product | Rules | Workflows | Memory / context |
|---------|-------|-----------|------------------|
| **LocalBoost** | [products/localboost/rules/](products/localboost/rules/) | [products/localboost/workflows/](products/localboost/workflows/) | [docs/memory-bank/localboost-*.md](../docs/memory-bank/) |
| **Consulting** | [products/consulting/rules/](products/consulting/rules/) | — | [docs/memory-bank/](../docs/memory-bank/) |

## Shared entry points

- [../AGENTS.md](../AGENTS.md) — monorepo agent guide (read first)
- [../docs/scaffolding.md](../docs/scaffolding.md) — product layout template
- [../docs/database/README.md](../docs/database/README.md) — Flyway migrations index

## LocalBoost (migrated from standalone repo)

Full rule set copied from `fixna-localboost`:

- `fixna-core.mdc`, `backend.mdc`, `frontend.mdc`, `database.mdc`, `api.mdc`
- `security-tenancy.mdc`, `testing.mdc`, `ai-llm.mdc`, `platform-adapters.mdc`
- `observability.mdc`, `git.mdc`, `agent-execution.mdc`

Workflows `00-project-bootstrap.md` through `14-logging-observability.md` in `products/localboost/workflows/`.
