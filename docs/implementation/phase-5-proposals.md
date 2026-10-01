# Phase 5 — Proposals

**Status:** Complete  
**Date:** 2026-09-28  
**Service:** `backend/consulting-api`  
**Frontend:** `frontend/consulting-web`

## Scope delivered

| Capability | Status | Location |
|------------|--------|----------|
| Proposal | Done | `consulting/proposal/` |
| ProposalItem | Done | `consulting/proposal/` |
| Send / approve / reject lifecycle | Done | `ProposalApplicationService` |
| Totals recalculation | Done | Subtotal + tax (0 MVP) → total |
| Domain events | Done | `ProposalCreated`, `ProposalSent`, `ProposalApproved`, `ProposalRejected` |
| Flyway V5 migration | Done | `V5__proposals.sql` |
| Frontend /proposals | Done | `frontend/consulting-web/src/app/proposals/` |

## Lifecycle states

| Status | Transitions | Who |
|--------|-------------|-----|
| DRAFT | → SENT, CANCELLED | PROPOSAL_MANAGE |
| SENT | → APPROVED, REJECTED, CANCELLED | APPROVE/REJECT: PROPOSAL_APPROVE; CANCEL: PROPOSAL_MANAGE |
| APPROVED | terminal | — |
| REJECTED | terminal | — |
| CANCELLED | terminal | — |

Send requires at least one line item (`PROPOSAL_EMPTY` otherwise).

## API endpoints

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/v1/proposals` | PROPOSAL_VIEW |
| POST | `/api/v1/proposals` | PROPOSAL_MANAGE |
| GET | `/api/v1/proposals/{id}` | PROPOSAL_VIEW |
| PUT | `/api/v1/proposals/{id}` | PROPOSAL_MANAGE |
| POST | `/api/v1/proposals/{id}/items` | PROPOSAL_MANAGE |
| PUT | `/api/v1/proposals/{id}/items/{itemId}` | PROPOSAL_MANAGE |
| DELETE | `/api/v1/proposals/{id}/items/{itemId}` | PROPOSAL_MANAGE |
| POST | `/api/v1/proposals/{id}/send` | PROPOSAL_MANAGE |
| POST | `/api/v1/proposals/{id}/approve` | PROPOSAL_APPROVE |
| POST | `/api/v1/proposals/{id}/reject` | PROPOSAL_APPROVE |
| POST | `/api/v1/proposals/{id}/cancel` | PROPOSAL_MANAGE |

Query param for list: `clientId`

## Tests

- `ProposalApplicationServiceTest` — create, send, approve RBAC, edit guards, not-found
- `ProposalLifecycleTest` — enum transition rules

## Next: Phase 7 — Client Portal

See [phase-6-invoicing.md](./phase-6-invoicing.md) (complete). Next up: Client Portal via ClientPortalMembership.
