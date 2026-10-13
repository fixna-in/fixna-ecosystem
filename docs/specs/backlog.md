# Backlog — Website Audit & Invoice Assistant (target design)

Sizing: **S** ≤ 0.5 day, **M** ≤ 1 day. Each task is independently reviewable; migrations get their own review when actually applied. Status: all **Proposed** pending approval of the [decision register](README.md).

## Website Audit (WB)

| ID | Task | Size | Depends | Acceptance |
|----|------|------|---------|------------|
| WB-01 | Resolver seam + pinned-IP connect in `WebsiteFetcher` (ADR-010) | M | approval | Rebinding unit test green; SSRF matrix extended |
| WB-02 | Crawl frontier + bounds (depth/pages/domain/robots/delay) | M | WB-01 | Bounds unit tests; url_hash dedupe |
| WB-03 | Job table migration + submit/cancel/list endpoints (202 model) | M | approval | Migration reviewed; API contract tests |
| WB-04 | Worker loop + lease heartbeat + startup recovery | M | WB-03 | Recovery tests: restart, expiry, retry cap |
| WB-05 | Per-page persistence (`website_audit_pages`) + partial reporting | S | WB-03 | Pages stored; checks immutable |
| WB-06 | Site-level checks (duplicate titles, thin content, internal links) | S | WB-02 | Golden fixtures |
| WB-07 | Self-contained HTML report export (escaped + CSP) | S | WB-03 | XSS escape tests |
| WB-08 | Frontend: async submit + job polling + progress UI | M | WB-03, WB-04 | Build green; polling backoff |
| WB-09 | Rate-limit route config for submit/cancel | S | WB-03 | 429 under load test |

## Invoice Assistant (IA)

| ID | Task | Size | Depends | Acceptance |
|----|------|------|---------|------------|
| IA-01 | Upload endpoint (magic bytes, size, `FileStorage`, sha256) | M | approval | Upload validation suite green |
| IA-02 | Document list/download/delete (opaque ids, streaming) | S | IA-01 | Cross-tenant 404 tests |
| IA-03 | Native text extraction adapter + provenance rows | M | IA-01 | Fixture PDFs; snippet/bbox asserted |
| IA-04 | OCR adapter interface + offline engine integration | M | IA-03 | Thin-text fallback; `OCR_UNAVAILABLE` path |
| IA-05 | Confidence scoring + gates + BigDecimal money grammar | M | IA-03 | Decimal + boundary tests |
| IA-06 | Review state machine + approve/reject/post endpoints | M | IA-05 | `GATES_UNMET`; `INVOICE_NOT_EDITABLE`; audit events |
| IA-07 | CSV/JSON export | S | IA-06 | Column registry; export audit event |
| IA-08 | PDF parser hardening (timeouts, memory caps, patch audit) | S | IA-01 | Bomb/malformed fixtures survive |
| IA-09 | Retention purge job (first `@Scheduled` in consulting-api) | S | IA-01 | `purge_after` scan; tombstone-only delete |
| IA-10 | Frontend: upload + review UI with provenance display | M | IA-06 | Build green; gate badges rendered |
| IA-11 | Four-eyes approval (approver ≠ uploader) — post-MVP | S | IA-06 | Feature-flagged; audit shows both actors |

## Docs / infra (INF)

| ID | Task | Size | Depends | Acceptance |
|----|------|------|---------|------------|
| INF-01 | Apply approved migrations (real Flyway files) + db README rows | S | WB-03, IA-01 | `DeploymentReadinessTest` threshold updated in same PR |
| INF-02 | CI migration threshold bump when versions land | S | INF-01 | Workflow `-ge` matches actual count |

## Out of scope (reserved)

Authenticated crawling, JS rendering/screenshots, cloud OCR, push/websockets, multi-currency conversion, card-data capture, client-portal sharing of audits/documents.
