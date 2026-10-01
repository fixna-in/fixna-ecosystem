# ADR-007: Independent Product Builds

## Status
Accepted (2026-09-28)

## Context
The monorepo hosts multiple products that must build and deploy independently. A root Maven reactor must not force developers to compile unrelated product code for day-to-day work.

## Decision

1. Each product API has its own `pom.xml` under `backend/{product}-api/`.
2. Shared library `fixna-platform-common:1.0.0` is a versioned Maven dependency.
3. Before building one product in isolation, bootstrap the local Maven repo:

   ```bash
   npm run bootstrap:maven
   ```

   This runs `mvn -f pom.xml install -N` (parent POM) and installs `fixna-platform-common`.

4. Full reactor build remains available: `mvn -f pom.xml clean test`.
5. Docker builds use monorepo context (`dockerContext: .`) with `-pl backend/{product}-api -am`.
6. Frontends build independently via `npm --prefix frontend/{product}-web run build`.

## Consequences

- CI installs parent POM before product jobs.
- No product-to-product Maven dependencies.
- Hospitality is excluded from build scripts until implemented.
