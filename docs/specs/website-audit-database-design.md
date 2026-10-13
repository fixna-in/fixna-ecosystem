# Database Design — Website Audit (proposed, NOT applied)

⚠️ **Design-only.** No Flyway file is created by this document. Implementation requires a separately reviewed migration in `backend/consulting-api/src/main/resources/db/migration/` (V12+ namespace reserved; exact version assigned at implementation time). Existing V10 tables are unchanged — no ALTERs anywhere (D12).

## Existing (shipped, V10)

- `website_audits`: id, tenant_id, profile_id?, input_url, normalized_url_hash, status, overall_score, summary, created_by_user_id, timestamps; indexes on tenant, tenant+created_at DESC, profile (partial).
- `website_audit_checks`: id, tenant_id, audit_id FK CASCADE, check_code, severity, score, evidence JSONB, fix_hint, created_at.

## Proposed: `website_audit_jobs`

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | `gen_random_uuid()` |
| tenant_id | UUID NOT NULL | FK tenants |
| audit_id | UUID | FK website_audits ON DELETE SET NULL — nullable (job may fail before an audit exists) |
| requested_url | TEXT NOT NULL | raw input |
| status | VARCHAR(20) NOT NULL | QUEUED/RUNNING/COMPLETED/FAILED/CANCELLED + CHECK |
| crawl_enabled | BOOLEAN NOT NULL DEFAULT true | |
| attempt | INT NOT NULL DEFAULT 0 | max 3 (ADR-009) |
| locked_by | VARCHAR(100) | worker id; NULL when unclaimed |
| lease_expires_at | TIMESTAMPTZ | heartbeat target |
| failure_code | VARCHAR(60) | per API registry |
| created_by_user_id | UUID NOT NULL | FK users |
| created_at / updated_at | TIMESTAMPTZ NOT NULL | |

Indexes: `(status, lease_expires_at)` for the recovery scan; `(tenant_id, created_at DESC)` for listing; partial `(tenant_id) WHERE status = 'RUNNING'` to support the one-per-tenant guard.

## Proposed: `website_audit_pages`

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| tenant_id | UUID NOT NULL | FK tenants |
| audit_id | UUID NOT NULL | FK website_audits CASCADE |
| job_id | UUID | FK website_audit_jobs ON DELETE SET NULL |
| url | TEXT NOT NULL | normalized |
| url_hash | VARCHAR(64) NOT NULL | dedupe within audit |
| depth | INT NOT NULL DEFAULT 0 | seed = 0 |
| http_status | INT | NULL if blocked |
| content_type | VARCHAR(100) | |
| size_bytes | INT | |
| fetch_ms | INT | |
| fetched_at | TIMESTAMPTZ | |

Constraints: `UNIQUE (audit_id, url_hash)`. Index `(audit_id)`.

## Invariants

- All rows tenant-scoped; every repository method filters by `tenant_id` (existing convention).
- No FKs between `websiteaudit` and `invoiceassist` tables (domain independence).
- Checks and pages are append-only per audit (immutability — product spec §3.3).
- Scores are application-computed; no triggers, no stored procedures.
- Storage growth is bounded by design: ≤ 25 pages + ≤ 14-ish checks per audit per run.
