# Test Plan — Website Audit & Invoice Assistant (target design)

Conventions: JUnit 5 + Mockito + AssertJ unit tests per service (existing pattern); Testcontainers integration tests exist but skip without Docker; frontend = Next.js build + zod schema parsing. No new test frameworks (D13: one combined plan).

## 1. Website Audit

### 1.1 Determinism (`WebsiteAuditor` golden tests)
- Fixed HTML fixtures → expected per-check scores + overall (exact); non-HTML / non-2xx skip rules; weight constants pinned by assertions.

### 1.2 Crawl bounds
- Frontier: depth cap, page cap (25), fetch-attempt cap (40), domain scope (subdomains excluded), url_hash dedupe, per-host delay honored (virtual clock), robots.txt respected including the 5s crawl-delay cap.

### 1.3 Fetcher security (SSRF matrix)
- Schemes/ports; private/loopback/link-local/CGNAT/ULA for IPv4 **and** IPv6; decimal/octal/hex IP literals; metadata host denylist; redirect refusal; **rebinding seam**: resolver returns public-then-private → connect refused at pin time; 512 KB cap; timeout paths.

### 1.4 Jobs & recovery
- CAS claim (only one worker wins); heartbeat extends lease; startup requeue marks RUNNING→QUEUED and preserves completed pages; retry cap 3 → FAILED(`RECOVERY_EXHAUSTED`); cancel from QUEUED and RUNNING; `409 CRAWL_IN_PROGRESS` per tenant; cross-tenant jobId → 404.

### 1.5 API contract
- 202 submit shape; job resource fields; report export 200/404; paged job list clamps; `ApiError` codes per registry; RBAC matrix (VIEW vs MANAGE vs AI_USE).

### 1.6 AI-optional
- With `AiProvider` throwing: crawl + checks + score + report succeed; `/fixes` → `FixnaException("AI_UNAVAILABLE")`; no `AiUsageLog` rows written on the failure path.

## 2. Invoice Assistant

### 2.1 Upload validation
- Magic-byte accept/reject; > 10 MB reject; non-PDF reject; traversal filenames sanitized; sha256 dedupe uniqueness.

### 2.2 Extraction & provenance
- Native-text PDF fixture → fields with page/snippet/bbox; thin-text fixture → OCR adapter invoked (mocked engine); `extractor_version` stamped; `MANUAL` source recorded on edits.

### 2.3 Decimal safety
- Accept: `1234.56`, `1.234,56`, `€1,234.56` (fixed locale set); reject: `12.345`, `1e3`, `NaN`, scale > 2; `NUMERIC(14,2)` round-trip; Σ-lines-vs-total flag.

### 2.4 Confidence gates
- Boundary tests at 0.90 / 0.70; low-confidence hidden; approve blocked with `GATES_UNMET` until confirmed; `AI_ADVISORY` never satisfies a gate.

### 2.5 State machine
- Legal/illegal transitions; post only `DRAFT`; `REJECTED` terminal; approve requires `INVOICE_MANAGE`; audit events emitted with `Map<String,String>` details only.

### 2.6 Export & retention
- CSV columns (key, value, confidence, source, page, snippet); JSON snapshot fidelity; `purge_after` computed from POSTED/VOID dates; delete → bytes gone + id-only tombstone.

### 2.7 RBAC & isolation
- Matrix across PLATFORM_ADMIN / CONSULTANT_ADMIN / CONSULTANT / CLIENT_ADMIN / CLIENT_USER; cross-tenant document/extraction → 404; tenant never taken from client input.

## 3. Shared/platform guards

- `DeploymentReadinessTest`: migration threshold + last-version assertion updated **in the same PR** as any new Flyway file.
- `PublicEndpointComplianceTest`: every new endpoint authenticated (none public).
- CI: consulting job only; migration check `-ge` updated in lockstep.

## 4. Frontend

- zod schemas round-trip fixtures (job resource, extraction payload, provenance); `/website-audits` list/detail build; invoices panel flows (upload → review → approve → post) tested against mocked api-client; `next build` green.

## 5. Exit criteria

All unit suites green (baseline 182 + new), no new compile warnings, SSRF matrix 100% pass, recovery tests deterministic (no real sleeps > 100 ms), frontend production build green.
