# ADR-009: Bounded Website Crawl Jobs with Lease Recovery

## Status

Proposed (2026-10-12) — pending approval ([decision register](../specs/README.md))

## Context

The Website Audit MVP fetches a single page synchronously (up to ~30s; the frontend uses a 45s timeout). Multi-page crawling cannot fit inside request timeouts, and consulting-api has no scheduler or queue infrastructure (`@Scheduled`/`@EnableAsync` are unused). Without a persisted job record there is no crash recovery: a server restart mid-audit silently loses work.

## Decision

1. Audit runs become **asynchronous jobs**: `POST /website-audits` → `202` with a job resource; the client polls until a terminal state.
2. New table `website_audit_jobs` (design-only in the specs; a Flyway version is created only when implementation is approved): states `QUEUED → RUNNING → COMPLETED | FAILED | CANCELLED`.
3. **Lease model:** a worker claims a QUEUED job via CAS update (sets `locked_by`, `lease_expires_at = now() + 90s`); a heartbeat extends the lease while crawling.
4. **Recovery:** on startup, a one-shot pass marks RUNNING jobs with expired leases as QUEUED again (attempt +1; after 3 attempts → FAILED with `RECOVERY_EXHAUSTED`). Recovery is idempotent — completed page progress in `website_audit_pages` is reused; only missing pages are fetched.
5. **Crawl bounds (hard limits, server-enforced, clamped regardless of client input):**
   - Max 25 stored pages per audit; max 40 fetch attempts (robots/sitemap consume attempts)
   - Max depth 2 (seed = depth 0); same registrable domain as the seed only
   - `robots.txt` honored; crawl-delay respected up to a 5s cap
   - Per page: 512 KB body cap, 10s connect/read timeouts (existing fetcher limits)
   - Wall-clock budget 60s per job attempt; per-host politeness delay 500ms
   - Exactly one RUNNING crawl per tenant (tenant-scoped CAS guard → `409 CRAWL_IN_PROGRESS`)
6. Scoring stays deterministic (`WebsiteAuditor`); job completion invokes the same scoring path as the MVP.

## Consequences

- Adds a single-threaded in-process worker loop; no external queue dependency (a managed queue can be adopted later with the job table as source of truth, outbox-style).
- API contract shifts from synchronous 201 to 202 + poll — frontend must adapt (backlog WB-08).
- Recovery requires dedicated tests: restart mid-job, lease expiry, retry cap, cancel races.
- Partial failures are visible: a FAILED job can still expose partial page results for diagnostics without publishing an "completed" audit.
