# API Contract — Website Audit (current + proposed)

Base: `/api/v1`. Errors: `ApiError` shape via `GlobalExceptionHandler`; domain errors via `FixnaException(code, status, message)`. All endpoints require JWT; tenant is always server-derived. Cross-tenant access → 404-style `NOT_FOUND` (never existence-revealing 403).

## Shipped (unchanged by spec approval itself)

| Method & path | Permission | Notes |
|---|---|---|
| `POST /website-audits` | `WEBSITE_AUDIT_MANAGE` | Sync MVP run (target: becomes async — D1) |
| `GET /website-audits` | `WEBSITE_AUDIT_VIEW` | List, newest first |
| `GET /website-audits/{id}` | `WEBSITE_AUDIT_VIEW` | Detail + checks |
| `POST /website-audits/{id}/fixes` | `WEBSITE_AUDIT_MANAGE` + `AI_USE` | Advisory AI |

## Proposed target contract

### Submit audit — `POST /website-audits` → 202

Request:
```json
{ "url": "https://example.com", "profileId": null, "crawl": true }
```
Response:
```json
{ "jobId": "uuid", "status": "QUEUED",
  "links": { "self": "/website-audits/jobs/{jobId}", "audit": null } }
```
Errors: `422 INVALID_URL`, `422 URL_BLOCKED_BY_POLICY` (SSRF), `409 CRAWL_IN_PROGRESS` (tenant already running), `429` rate-limited.

### Job resource — `GET /website-audits/jobs/{jobId}` → 200

```json
{
  "jobId": "uuid",
  "status": "QUEUED|RUNNING|COMPLETED|FAILED|CANCELLED",
  "attempt": 1,
  "maxAttempts": 3,
  "progress": { "pagesFetched": 7, "pagesStored": 7, "budgetPages": 25, "budgetSeconds": 60 },
  "failureCode": null,
  "links": { "audit": "/website-audits/{auditId}" }
}
```
`failureCode` ∈ `FETCH_FAILED | TIME_BUDGET_EXCEEDED | ROBOTS_BLOCKED | RECOVERY_EXHAUSTED | INTERNAL`.

### Cancel — `POST /website-audits/jobs/{jobId}/cancel` → 200 `{ "status": "CANCELLED" }`

Errors: `409 JOB_NOT_CANCELLABLE` when terminal.

### Job list — `GET /website-audits/jobs?status=&page=&size=` → 200

Paged `{items[], page, size, totalElements, totalPages}`; size clamped ≤ 50 (existing convention).

### Report export — `GET /website-audits/{id}/report.html` → 200 `text/html`

Self-contained HTML; `404 NOT_FOUND` cross-tenant.

### Advisory fixes (existing, unchanged) — `POST /website-audits/{id}/fixes` → 200

`AiSuggestionResponse<WebsiteAuditFixSuggestion>`; `503 AI_UNAVAILABLE` when the provider is down; max 6 suggestions.

## Conventions

- Polling guidance: job endpoint at ~2s intervals with backoff; no push/websocket in scope.
- All list endpoints tenant-scoped via repository signatures `findBy*AndTenantId`.
- New error codes are registered in the threat model/test plan before implementation.
