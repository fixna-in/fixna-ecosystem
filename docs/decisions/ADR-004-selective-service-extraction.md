# ADR-004: Selective Service Extraction

## Status
Accepted

## Context
Microservices add operational cost without proven scaling boundaries.

## Decision
Extract a module to an independent service only when there is a demonstrated need (scaling, ownership, deployment cadence). Default path: modular monolith → identify boundary → extract.

## Consequences
- No distributed-system complexity in MVP
- Package boundaries must be maintained to enable future extraction
