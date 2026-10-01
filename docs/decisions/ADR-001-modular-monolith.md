# ADR-001: Modular Monolith

## Status
Accepted

## Context
Fixna must support multiple products (LocalBoost, Consulting, Hospitality) without premature microservice complexity.

## Decision
Build each product as a modular monolith: one deployable API with clear package-level boundaries between platform core and product domains.

## Consequences
- Simpler deployment and transactions for MVP
- Package boundaries enforce modularity; extraction to services is deferred until demonstrated need
