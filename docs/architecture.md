# Architecture

Structured architecture documentation for the Fixna ecosystem.

| Document | Topic |
|----------|-------|
| [ecosystem.md](./architecture/ecosystem.md) | Overview and principles |
| [product-boundaries.md](./architecture/product-boundaries.md) | LocalBoost / Consulting / Hospitality isolation |
| [shared-platform.md](./architecture/shared-platform.md) | `fixna-platform-common` library |
| [database-isolation.md](./architecture/database-isolation.md) | Per-product databases and Flyway |
| [deployment.md](./architecture/deployment.md) | Render, Vercel, Docker, CI |

## Products

| Product | Doc |
|---------|-----|
| LocalBoost | [products/localboost.md](./products/localboost.md) |
| Consulting | [products/consulting.md](./products/consulting.md) |
| Hospitality | [products/hospitality.md](./products/hospitality.md) (placeholder) |

## Decisions (ADRs)

| ADR | Topic |
|-----|-------|
| [ADR-001](./decisions/ADR-001-modular-monolith.md) | Modular monolith per product |
| [ADR-002](./decisions/ADR-002-shared-platform.md) | Shared platform core strategy |
| [ADR-003](./decisions/ADR-003-event-strategy.md) | In-process events (MVP) |
| [ADR-007](./decisions/ADR-007-independent-product-builds.md) | Independent Maven/npm builds |
| [ADR-008](./decisions/ADR-008-database-isolation.md) | Database isolation |

See also [scaffolding.md](./scaffolding.md) and [build-and-deployment.md](./build-and-deployment.md).
