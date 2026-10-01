# ADR-002: Shared Platform Core

## Status
Accepted

## Context
Multiple Fixna products need identity, tenancy, RBAC, audit, notifications, files, and observability.

## Decision
Platform core lives in `in.fixna.platform` packages (identity, tenancy, membership, rbac, audit, etc.). Product-specific domains (e.g. consulting client, project) stay in separate packages and must not leak into platform core.

## Consequences
- Consulting API owns its platform copy initially; extraction to a shared JAR is a future optimization
- LocalBoost production code is not modified during Consulting build
