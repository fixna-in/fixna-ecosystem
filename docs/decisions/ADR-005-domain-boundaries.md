# ADR-005: Domain Boundaries

## Status
Accepted

## Context
Generic cross-product domain models (GenericCustomer, GenericOrder) create coupling and poor fit for vertical workflows.

## Decision
Each product owns its domain model. Platform provides technical capabilities only. LocalBoost owns Campaign/Lead; Consulting owns Client/Project/Invoice; Hospitality will own Property/Reservation.

## Consequences
- No shared business tables across products
- User ≠ Client; client portal access via ClientPortalMembership (Consulting)
