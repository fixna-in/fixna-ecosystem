# ADR-006: Consulting Billing Boundary

## Status
Accepted

## Context
LocalBoost uses subscription billing (Plan, Subscription). Consulting uses client invoice billing (Engagement → Invoice → Payment).

## Decision
Shared billing primitives (Money, Currency, Tax, PaymentMethod) may live in platform. Consulting invoice lifecycle is separate from LocalBoost subscription billing. Never merge into one vague billing model.

## Decision
Consulting billing flow: Client → Engagement → Invoice → InvoiceLine → Payment via PaymentProvider abstraction (ManualPaymentProvider initially).

## Consequences
- Invoice and subscription models remain separate
- Payment provider implementations stay outside domain entities
