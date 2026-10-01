# Fixna Ecosystem

Multi-product monorepo: independently deployable products sharing a controlled Java platform library.

```text
                         FIXNA ECOSYSTEM
                               |
             +-----------------+------------------+
             |                 |                  |
        LocalBoost         Consulting        Hospitality
        Web + API          Web + API          (future)
             |                 |
        DB: fixna          DB: consulting
             \________________|_________________/
                               |
              backend/libs/fixna-platform-common
```

## Principles

| Principle | Implementation |
|-----------|----------------|
| Modular monolith per product | One Spring Boot API + one Next.js app per product |
| No product coupling | LocalBoost and Consulting never import each other's domain code |
| Shared infra only | `fixna-platform-common` holds web/security/logging — not business domains |
| Separate databases | Each product owns Flyway migrations and PostgreSQL database |
| Independent deploy | `render-{product}.yaml` + Vercel per frontend |

## Repository layout

See [../scaffolding.md](../scaffolding.md) and root [README.md](../../README.md).

## Related docs

- [Product boundaries](./product-boundaries.md)
- [Shared platform library](./shared-platform.md)
- [Database isolation](./database-isolation.md)
- [Deployment](./deployment.md)
