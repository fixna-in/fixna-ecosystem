# ADR-003: Event Strategy

## Status
Accepted

## Context
Domain events (ClientCreated, InvoiceSent, etc.) must be decoupled from side effects without introducing Kafka in MVP.

## Decision
Use in-process domain event publishing with stable event contracts. Application services publish events; handlers run synchronously in-process. Outbox + Kafka is a future evolution.

## Consequences
- Simple MVP implementation
- Event contracts must be stable for future async migration
