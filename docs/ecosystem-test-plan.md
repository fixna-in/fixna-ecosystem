# Ecosystem Test Plan (2026-10-09)

## Automated — executed this pass

| Suite | Command | Result |
|---|---|---|
| platform-common | `mvn -f backend/libs/fixna-platform-common/pom.xml test` | 7 pass (baseline) |
| localboost-api | `mvn -f backend/localboost-api/pom.xml test` | 229 pass, 0 failures |
| consulting-api | `mvn -f backend/consulting-api/pom.xml test` | **148 pass, 0 failures** (136 baseline + 12 new rate-limit tests) |
| fixna-web unit | `npm --prefix frontend/fixna-web run test` | **21 pass, 2 files** (17 contact-handler + 4 site-url) |
| fixna-web typecheck | `npm --prefix frontend/fixna-web run typecheck` | pass (tsc clean) |
| production builds | `npm run build:all` (executed 2026-10-10) | **all 3 webs build** (fixna 17 routes incl. dynamic /api/contact; localboost 11; consulting 15) |

New tests added this pass:
- Consulting `RateLimiterTest` (5) + `RateLimitFilterTest` (6, incl. portal
  auth path + concurrency quota).
- fixna-web `handler.test.ts` (validation, honeypot, rate limit, storage
  failure without leaking internals, misconfiguration, method/JSON errors)
  and `site-url.test.ts` (4).

Docker unavailable locally → Testcontainers context-load tests skip
(`disabledWithoutDocker`); they run in CI.

## Automated — added to CI (validates on next push)

- `fixna-web` job: typecheck → vitest → production build.
- Existing jobs: platform-common, both APIs (tests + package + migration
  count), both product webs (production build).

## Runtime checks executed (2026-10-09 ~23:55 IST)

- `https://www.fixna.in/` — LIVE, content matches repo (pre-deploy state).
- `https://localboost.fixna.in/` — LIVE landing, mock-adapter disclosure
  present; API status probe resolves client-side.
- `https://consulting.fixna.in/` — LIVE landing.
- `https://api.fixna.in/api/v1/health` — **HTTP 503 (twice)**.
- `https://api.consulting.fixna.in/api/v1/health` — **HTTP 503 (twice)**.

## Manual / runtime (needs deployed envs + creds — OWNER)

- fixna.in after deploy: submit /api/contact → Sheet row appears; 429 on
  rapid repeat; inline validation on bad email; legal pages.
- localboost.fixna.in (once API is healthy): register -> login -> reload
  (session restore) -> create business -> create campaign (mock adapter
  labelled) -> leads transitions -> sign out -> protected routes.
- consulting.fixna.in (once API is healthy): register -> login -> reload ->
  client + project -> meeting validation -> proposal + invoice drafts,
  totals, lifecycle -> publish profile -> AI suggestion + provider-failure
  path; auth login 429 after quota.
- Render dashboards: investigate both 503 APIs (P1-5).
- app.fixna.in Vercel mapping for fixna-web (P2-4).
- Key rotation for the stray `git` key if it was ever pushed (P0-1).

## Non-goals

No prod data touched; isolated test tenants only. No live ad spend, no
gateway charges, no real email sends asserted. Missing creds/infra are
recorded as limitations, never claimed as passes.
