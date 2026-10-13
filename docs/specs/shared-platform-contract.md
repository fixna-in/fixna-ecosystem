# Shared Platform Contract Compatibility — Website Audit & Invoice Assistant

Scope: `backend/consulting-api` only. Rule: shared concerns are specified **only where existing contracts are compatible** — no changes to `backend/libs/fixna-platform-common`, no cross-product coupling.

## Identity & tenancy

- All endpoints require JWT auth; tenant comes from `TenantContext.requireTenantId()` (server-side only, never client-supplied).
- Actor comes from `TenantContext.requireUserId()`.
- Cross-tenant access returns 404-style `FixnaException("NOT_FOUND", ...)` — never a 403 that reveals existence.
- No new identity tables; platform primitives are per-product (ADR-008).

## Entitlement (RBAC + rate limits only)

Consulting has no plan/quota/subscription system — entitlement is the `Permission` enum plus route rate limiting. Shipped grants (V10/V11):

| Permission | Grants |
|---|---|
| `WEBSITE_AUDIT_VIEW` | list/detail audits |
| `WEBSITE_AUDIT_MANAGE` | run audits, advisory fixes, (target: submit/cancel jobs) |
| `INVOICE_ASSIST_USE` | suggestions, apply, dismiss |
| `INVOICE_MANAGE` | (target) upload/download/review/approve/post/delete/export documents |
| `AI_USE` | any AI path (additive requirement) |

**Decision (D11): no new permissions.** Target features reuse the five above. LocalBoost's `Subscription`/`PlanLimits` are a different product's billing model and are explicitly excluded.

## Usage

- AI calls → `ai_usage_logs` via the existing pattern (`tenantId`, `userId`, `AiFeature`, `request_summary`, `created_at`). No new `AiFeature` values required for the target design; if AI-assisted structuring is ever enabled it adds `INVOICE_DOCUMENT_EXTRACT` in its own reviewed change.
- Non-AI usage (audit runs, uploads, extractions) → `audit_logs` events only. No new usage counters in this design.

## Audit events (`AuditEvent` — details must be `Map<String, String>`)

| Existing | Proposed |
|---|---|
| `website.audit.completed`, `ai.suggestion.generated` | `website.audit.queued\|started\|completed\|failed\|cancelled` |
| `invoice.assist.generated\|applied\|dismissed` | `invoice.document.uploaded\|extracted\|deleted`, `invoice.review.rejected\|approved\|posted`, `invoice.export.generated` |

## Observability

- Errors: `FixnaException(code, status, message)` → `ApiError` via `GlobalExceptionHandler` (no stacks, no secrets).
- Logging: MDC via `TenantScopeProvider` (`tenantId`, `userId`, `traceId`, `requestId`); named loggers `fixna.audit`, `fixna.error`, `fixna.ratelimit`. Proposed: `fixna.jobs` for job lifecycle — product-local logger, no shared-lib change.
- Health: no change until jobs exist; then one readiness detail for the worker loop.
- Rate limits: submit/extract routes must be present in the existing `RateLimitFilter` route config (config-only change when implemented).

## AI-optional guarantee

Deterministic paths never call `AiProvider`. AI endpoints wrap provider failure as `FixnaException("AI_UNAVAILABLE", 503, ...)`. UI treats AI panels as optional. `MockAiProvider` remains the test seam. (Matches shipped behavior: scoring, crawling, extraction, validation, review, export, retention never touch AI.)

## Database independence

New tables (proposed, design-only) belong to the consulting Flyway history only: `website_audit_jobs`, `website_audit_pages`, `invoice_documents`, `invoice_document_extractions`, `invoice_document_fields`. No FKs between the `websiteaudit` and `invoiceassist` domains; each references only `tenants`, `users`, and its own domain tables (`consulting_profiles`, `invoices`). LocalBoost schema untouched.
