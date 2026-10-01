# Shared platform library

**Path:** `backend/libs/fixna-platform-common/`  
**Version:** `1.0.0` (Maven artifact `in.fixna:fixna-platform-common`)

## Purpose

Reusable Java infrastructure shared by all Fixna API products. Products depend on this library; the library never depends on product code.

## What it contains today

| Package | Responsibility |
|---------|----------------|
| `common.web` | `ApiError`, `GlobalExceptionHandler`, `RequestIdFilter`, `SecurityHeadersFilter`, auth entry points |
| `common.config` | CORS, password encoder config |
| `common.logging` | MDC context, sensitive data masking, `TenantScopeProvider` interface |
| `common.autoconfigure` | Spring Boot auto-configuration |

## What it does NOT contain

- Identity, RBAC, JWT services (each product implements its own `identity/` package today)
- Domain entities (Campaign, Client, Proposal, etc.)
- Flyway migrations
- Product-specific AI or billing logic

Extracting more platform code into this library is incremental; ADR-002 documents the current split.

## Dependency direction

```
fixna-platform-common  (no product imports)
         ↑
    ┌────┴────┐
localboost-api  consulting-api
```

## Local development

Product POMs reference the library via Maven dependency management. Before building a single product in isolation:

```bash
npm run bootstrap:maven
# or: scripts/bootstrap-maven.sh
```

This installs the parent POM and `fixna-platform-common:1.0.0` into `~/.m2`.

Alternatively, build the full reactor:

```bash
mvn -f pom.xml clean test
```

## Product bridges

Each API registers a `TenantScopeProvider` bridge so shared logging can read tenant/user from product `TenantContext`:

- LocalBoost: `LocalboostTenantScopeBridge`
- Consulting: `ConsultingTenantScopeBridge`

See [ADR-002](../decisions/ADR-002-shared-platform.md) and [ADR-007](../decisions/ADR-007-independent-product-builds.md).
