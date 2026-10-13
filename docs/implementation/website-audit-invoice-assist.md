# Website Audit & Invoice Assistant (Consulting)

Consulting-only features. LocalBoost is untouched.

## Website Audit (deterministic + advisory AI)

- **Endpoints:** `POST /api/v1/website-audits` (run), `GET /api/v1/website-audits` (list), `GET /api/v1/website-audits/{id}` (detail with checks), `POST /api/v1/website-audits/{id}/fixes` (advisory AI)
- **Permissions:** `WEBSITE_AUDIT_VIEW` (list/detail), `WEBSITE_AUDIT_MANAGE` (run/fixes); `AI_USE` additionally required for fixes
- **Scoring** is fully deterministic in `WebsiteAuditor` — no AI. Checks: `http_status`, `https`, `title`, `meta_description`, `viewport`, `open_graph`, `canonical`, `h1_present`, `img_alt`, `charset`, `html_lang`, `response_time`, `robots_txt`, `sitemap_xml`. Overall = rounded average; HTML checks are skipped (not failed) for non-HTML or non-2xx responses.
- **Fetching** (`WebsiteFetcher`): http/https only, ports 80/443 only, DNS resolution checked against loopback/private/link-local/CGNAT/unique-local ranges, no redirect following, 512 KB body cap, hard timeouts. Known limitation: DNS rebinding between check and request is not mitigated (acceptable for advisory MVP).
- **AI** is used only for `POST /{id}/fixes` (feature `WEBSITE_AUDIT_FIXES`) — advisory, logged to `ai_usage_logs`, audited via `website.audit.completed` / `ai.suggestion.generated` events.
- Audits link to the consulting profile when the profile `websiteUrl` host matches the audited host.

## Invoice Assistant (deterministic-first + advisory AI)

- **Endpoints:** `GET/POST /api/v1/invoices/{invoiceId}/assist/suggestions`, `POST /api/v1/assist/suggestions/{suggestionId}/apply`, `POST /api/v1/assist/suggestions/{suggestionId}/dismiss`
- **Permission:** `INVOICE_ASSIST_USE` on every call; `AI_USE` additionally required when AI line ideas are requested
- **Deterministic suggestions:** `MISSING_LINES` (no line items), `DUE_DATE_RECOMMENDED` (draft invoice without due date; payload carries a UTC-based net-14 date), `OVERDUE_REMINDER` (sent/partially-paid invoice past due date)
- **Auto-apply policy:** only `DUE_DATE_RECOMMENDED` can be applied, only to editable (`DRAFT`) invoices; anything else returns `SUGGESTION_NOT_APPLICABLE` / `INVOICE_NOT_EDITABLE`
- **AI line ideas** (`LINE_IDEAS`, feature `INVOICE_LINE_IDEAS`) are advisory-only — stored as a suggestion with `payload.source = ai_advisory`, never applied automatically
- Open (`SUGGESTED`) suggestions are de-duplicated per type; dismissed/applied suggestions allow regeneration
- Audit events: `invoice.assist.generated`, `invoice.assist.applied`, `invoice.assist.dismissed`, `ai.suggestion.generated`

## Migrations

- `V10__website_audit.sql` — `website_audits`, `website_audit_checks`
- `V11__invoice_assist.sql` — `invoice_assist_suggestions`

## Frontend

- `/website-audits` (list + run), `/website-audits/[id]` (checks + advisory AI fixes)
- Invoice assistant panel on `/invoices` (analyze, apply due date, dismiss, optional AI line ideas)
- API layers: `src/lib/website-audit-api.ts`, invoice-assist functions in `src/lib/invoices-api.ts` (zod-validated)
