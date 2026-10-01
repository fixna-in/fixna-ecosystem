# Phase 6 — Invoicing

**Status:** Complete  
**Date:** 2026-09-28  
**Service:** `backend/consulting-api`  
**Frontend:** `frontend/consulting-web`

## Scope delivered

| Capability | Status | Location |
|------------|--------|----------|
| Invoice | Done | `consulting/invoice/` |
| InvoiceLine | Done | `consulting/invoice/` |
| Payment | Done | `consulting/invoice/Payment.java` |
| PaymentProvider abstraction | Done | `billing/PaymentProvider.java` |
| ManualPaymentProvider | Done | `billing/ManualPaymentProvider.java` |
| Create from approved proposal | Done | `proposalId` on create copies line items |
| Flyway V6 migration | Done | `V6__invoicing.sql` |
| Frontend /invoices | Done | `frontend/consulting-web/src/app/invoices/` |

## Billing flow (ADR-006)

```
Client → Engagement → Invoice → InvoiceLine → Payment (ManualPaymentProvider)
                         ↑ optional link from approved Proposal
```

## Lifecycle states

| Status | Transitions |
|--------|-------------|
| DRAFT | → SENT, CANCELLED |
| SENT | → PARTIALLY_PAID, PAID, CANCELLED |
| PARTIALLY_PAID | → PAID, CANCELLED |
| PAID | terminal |
| CANCELLED | terminal |

Payments auto-transition invoice status based on `amountPaid` vs `totalAmount`.

## API endpoints

| Method | Path | Permission |
|--------|------|------------|
| GET | `/api/v1/invoices` | INVOICE_VIEW |
| POST | `/api/v1/invoices` | INVOICE_MANAGE |
| GET | `/api/v1/invoices/{id}` | INVOICE_VIEW |
| PUT | `/api/v1/invoices/{id}` | INVOICE_MANAGE |
| POST | `/api/v1/invoices/{id}/lines` | INVOICE_MANAGE |
| DELETE | `/api/v1/invoices/{id}/lines/{lineId}` | INVOICE_MANAGE |
| POST | `/api/v1/invoices/{id}/send` | INVOICE_MANAGE |
| POST | `/api/v1/invoices/{id}/cancel` | INVOICE_MANAGE |
| GET | `/api/v1/invoices/{id}/payments` | PAYMENT_VIEW |
| POST | `/api/v1/invoices/{id}/payments` | INVOICE_MANAGE |

## Domain events

- `InvoiceCreated`, `InvoiceSent`, `PaymentRecorded`, `InvoicePaid`

## Tests

- `InvoiceApplicationServiceTest` — create, send, payment, RBAC, guards
- `ManualPaymentProviderTest` — amount validation
- `InvoiceLifecycleTest` — enum transition rules

## Next: Phase 7 — Client Portal

Client-facing portal auth and views via `ClientPortalMembership`.
