# Threat Model — Website Audit (Consulting)

Assets: tenant data & tenancy boundary, server network position (SSRF), availability, AI spend, report integrity.
Trust: the authenticated consultant is trusted-with-boundaries; the target website, its content, and the AI provider are **untrusted**.

| # | Threat | Impact | Mitigations (existing → target) | Residual |
|---|--------|--------|--------------------------------|----------|
| T1 | SSRF to internal services (cloud metadata, intranet) | Critical | Existing: scheme/port allowlist, DNS private-range validation, no redirects → Target: pinned-IP connect (ADR-010), metadata-literal denylist | Low |
| T2 | DNS rebinding | Critical | Target: resolve-once / validate-all / pin-connect, TTL ≤ 30s, resolver test seam (ADR-010) | Low |
| T3 | Crawl bomb, zip bomb, oversized page | High | Existing: 512 KB/page → Target: page/fetch/time budgets, depth+domain bounds, 10s timeouts (ADR-009) | Low |
| T4 | robots.txt/sitemap loop or URL trap | Medium | Target: fetch-attempt cap (40), url_hash dedupe, per-host 500ms delay | Low |
| T5 | Hostile content in evidence/snippets (stored XSS via report) | High | Snippets ≤ 300 chars, escaped at render; HTML report self-contained + CSP `default-src 'none'`; frontend never uses `dangerouslySetInnerHTML` | Low |
| T6 | Cross-tenant audit/job/report access | High | `findBy*AndTenantId` everywhere; 404 on mismatch; tests extended to jobs and report export | Low |
| T7 | Audit-run abuse (availability/DoS via mass crawls) | Medium | `WEBSITE_AUDIT_MANAGE` gate, RateLimitFilter route config, 1 RUNNING crawl/tenant, hard bounds | Low |
| T8 | AI spend escalation via /fixes loops | Medium | `AI_USE` + `ai_usage_logs` + rate limiting; fixes capped at 6 suggestions (shipped) | Low |
| T9 | Secrets/PII in logs or evidence | High | Evidence stores only publicly fetched content; `SensitiveDataMasker` on logs; fetcher sends no cookies/headers and stores none | Low |
| T10 | Job hijack across tenants | Medium | jobId → tenant-scoped lookup; 404 cross-tenant; cancel/approve paths re-check permission | Low |
| T11 | Partial/corrupt reports after crash | Medium | Lease recovery + idempotent page reuse; failed jobs never publish a COMPLETED audit | Low |

## Review cadence

Re-threat-model when: crawling scope grows (authenticated pages, JS rendering), report export moves to email/object storage, or a real external queue/scheduler is introduced.
