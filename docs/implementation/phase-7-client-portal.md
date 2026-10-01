# Phase 7 — Client Portal

**Status:** Complete  
**Version:** 0.7.0  
**Date:** 2026-09-28

## Goal

Give client users a dedicated portal auth path and read-only (plus proposal approve/reject) views scoped to their client.

## Backend

### Migration `V7__client_portal.sql`
- `refresh_tokens.client_id` — optional FK to `clients` for portal sessions
- Index on `client_portal_memberships(user_id)`

### Auth
- JWT access/refresh tokens carry optional `clientId` claim
- `POST /api/v1/portal/auth/login` — email + password + `clientId`
- Shared `/api/v1/auth/refresh` rotates portal tokens when `client_id` is stored on refresh record
- `MembershipLookup` resolves portal roles from `ClientPortalMembership` when token has `clientId`
- `TenantContext.requireClientId()` enforces portal scope

### Portal API (`/api/v1/portal/*`)
| Endpoint | Permission | Notes |
|----------|------------|-------|
| `GET /me` | portal scope | Client context + role |
| `GET /projects` | PROJECT_VIEW | Client-scoped |
| `GET /proposals` | PROPOSAL_VIEW | Hides DRAFT/CANCELLED |
| `GET /proposals/{id}` | PROPOSAL_VIEW | Client ownership check |
| `POST /proposals/{id}/approve` | PROPOSAL_APPROVE | CLIENT_ADMIN only |
| `POST /proposals/{id}/reject` | PROPOSAL_APPROVE | CLIENT_ADMIN only |
| `GET /invoices` | INVOICE_VIEW | Hides DRAFT |
| `GET /invoices/{id}` | INVOICE_VIEW | Includes lines + payments |
| `GET /meetings` | MEETING_VIEW | Client-scoped |

## Frontend

- `/portal` — portal login (email, password, client ID) and tabbed views
- `src/lib/portal-api.ts`

## Tests

- `PortalApplicationServiceTest` — visibility, tenant/client isolation, RBAC
- `JwtServiceTest` — clientId claim round-trip
- `TenantContextTest` — portal client scope

## Next: Phase 8 — Public Website
