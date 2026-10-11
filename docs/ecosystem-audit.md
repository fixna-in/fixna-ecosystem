# Ecosystem Audit — Fixna Ecosystem

Date: 2026-10-09 · Author: senior full-stack review (Act mode, Stage 1)
Scope: fixna-web, localboost-web, consulting-web, localboost-api,
consulting-api, fixna-platform-common, docker-compose.yml, render blueprints,
CI (.github/workflows/ci.yml).

Status legend: Confirmed defect · Design/usability issue ·
Needs runtime verification · Enhancement · Out of scope.
Each finding carries a **Status** (Open / Fixed / Verified) reflecting the
2026-10-09 implementation pass.

## 1. Page / route inventory (verified from disk)

### fixna-web (corporate site — 11 routes, all implemented)

| Route | File | Notes |
|---|---|---|
| `/` | `src/app/page.tsx` | HomePage |
| `/about` | `src/app/about/page.tsx` | — |
| `/capabilities` | `src/app/capabilities/page.tsx` | legacy duplicate of /what-we-do; now canonicalized + noindex |
| `/what-we-do` | `src/app/what-we-do/page.tsx` | canonical capabilities route (nav, footer, homepage CTA) |
| `/products` | `src/app/products/page.tsx` | — |
| `/consulting` | `src/app/consulting/page.tsx` | — |
| `/technology` | `src/app/technology/page.tsx` | — |
| `/industries` | `src/app/industries/page.tsx` | — |
| `/contact` | `src/app/contact/page.tsx` + `contact-form.tsx` | POST /api/contact (Sheets) + mailto fallback |
| `/privacy-policy` | `src/app/privacy-policy/page.tsx` | — |
| `/terms` | `src/app/terms/page.tsx` | — |
| `POST /api/contact` | `src/app/api/contact/route.ts` | serverless handler (handler.ts + sheets.ts) |

SEO: per-page metadata, `sitemap.ts` (now includes all 10 public content
routes), `robots.ts`, `site-url.ts` (canonical base) all present.

### localboost-web (10 routes)

Landing `/` (public, HealthProbe), `/login`, `/register`, `/dashboard`,
`/businesses`, `/businesses/[id]`, `/campaigns`, `/campaigns/new`,
`/campaigns/[id]`, `/leads`. Protected pages gate on session with
loading/empty/error states.

### consulting-web (12 routes)

Landing `/`, `/login`, `/register`, `/portal` (client-portal session),
`/clients`, `/projects`, `/meetings`, `/proposals`, `/invoices`, `/ai`
(advisory suggestions), `/website` (private profile editor),
`/site/[slug]` (public profile). Detail views are inline/dialog-based, not
separate [id] routes.

### Backend surface

Consulting + LocalBoost APIs serve REST under `/api/v1/*` with JWT + RBAC +
tenant scoping. Public: `/auth/*`, `/portal/auth/login` (Consulting only),
`/public/**` (Consulting only), `/health/**`, actuator, swagger.
Everything else authenticated.

## 2. Findings

### P0

- **P0-1** (Confirmed, security): stray file named `git` at repo root —
  treated as a possibly-exposed private key; `.gitignore` hardened with
  secret-pattern rules (`git`, `*.pem`, `*.key`, `*-key.json`,
  `*service-account*.json`, …). **Status: Fixed** (ignore rules); key
  rotation on GitHub remains an owner action.
- **P0-2** (Confirmed, security): Consulting login had NO rate limiting.
  **Status: Fixed** — `RateLimiter` + `RateLimitFilter` ported into
  consulting-api (covers `/api/v1/auth/**` AND `/api/v1/portal/auth/**`),
  wired into `SecurityConfig`, `fixna.security.rate-limit.per-ip-per-minute`
  (env `FIXNA_RATE_LIMIT_PER_IP_PER_MINUTE`, default 20) added to
  application.yml, 10 new tests. Suite: 148 tests, 0 failures.
- **P0-3** (Design/usability): LocalBoost landing `HealthProbe` rendered a
  raw error banner for first-time visitors when the API was down.
  **Status: Fixed** — degrades to a neutral `role="status"` note.

### P1

- **P1-1** (Confirmed, duplicate content): BOTH `/capabilities` and
  `/what-we-do` existed, showing the same capability data; nav pointed at
  `/capabilities` while homepage CTA + footer pointed at `/what-we-do`.
  **Status: Fixed** — nav now uses `/what-we-do`; `/capabilities` stays
  live with `canonical=/what-we-do` + `noindex`; sitemap updated.
- **P1-2** (Confirmed, brief 2.5): contact page was mailto-only.
  **Status: Fixed** — `POST /api/contact` Next.js route handler appends to
  a Google Sheets spreadsheet via a service account; server-side validation,
  honeypot, per-IP fixed-window rate limit (default 3/10 min), safe generic
  errors, explicit "recorded" (never "emailed") confirmation, mailto
  fallback link retained; 17 Vitest cases. Requires one-time owner setup:
  sheet + service account + two Vercel env vars (see .env.example).
- **P1-3** (Confirmed, process): fixna-web had no tests, no CI job.
  **Status: Fixed** — Vitest harness + `fixna-web` CI job
  (typecheck → test → production build).
- **P1-4** (Verified design): Consulting AI = MockAiProvider only
  (deterministic templates; proposal draft uses hardcoded 2500/7500/1500
  line items). No key exposure; AI-suggested prices are fabricated by
  design and stay behind human review. Product cards on the corporate site
  now state this (`statusNote`). **Status: Verified/disclosed.**
- **P1-5** (Confirmed at runtime 2026-10-09 ~23:55 IST): BOTH
  `api.fixna.in/api/v1/health` AND `api.consulting.fixna.in/api/v1/health`
  return HTTP 503 from the live deployment (two probes each). The web apps
  (www.fixna.in, localboost.fixna.in, consulting.fixna.in) are up. Likely
  Render-side (asleep/crashed/misconfigured services) — requires owner
  action on the Render dashboards; not fixable from the repo.
  **Status: Open (infra — owner).**

### P2

- **P2-1** (Confirmed, cosmetic): inconsistent trailing slash on Consulting
  product link. **Status: Fixed** (`product-links.ts` + `products.ts`).
- **P2-2** (Confirmed, hygiene): stale compiled tree `services/consulting-api/`
  indexed by search; root `.gitignore` covered only `backend/*/target/`.
  **Status: Fixed** — `.gitignore` now ignores `target/` globally.
- **P2-3** (Verified design): `site-url.ts` falls back to
  `http://localhost:3002` when `NEXT_PUBLIC_SITE_URL` is unset — canonicals,
  sitemap and OG URLs then point at localhost. **Status: Open** — deployment
  must set `NEXT_PUBLIC_SITE_URL=https://www.fixna.in` (documented in
  .env.example and deployment steps).
- **P2-4** (Needs runtime verification): docs reference `app.fixna.in`
  for fixna-web; Vercel project/root mapping unconfirmed. **Status: Open
  (runtime).**
- **P2-5** (Confirmed, content accuracy): privacy policy described vague
  "standard analytics" that do not exist. **Status: Fixed** — now states no
  third-party analytics scripts, Vercel log processing, and Google Sheets
  storage of contact submissions. Legal review still recommended
  (not legal advice).
- **P2-6** (Confirmed, consistency): sitemap omitted `/industries`,
  `/privacy-policy`, `/terms`, `/what-we-do`. **Status: Fixed.**
- **P2-7** (Confirmed, product-status honesty): product cards showed
  LIVE/MVP/COMING SOON without integration reality. **Status: Fixed** —
  `statusNote` on every card (mock ad adapters, advisory AI, planned
  product).

## 3. Verified healthy (no action)

- Tenant isolation: `TenantContext` (both APIs) populated from the JWT only;
  repository queries are tenant-scoped; LocalBoost has `CrossTenantSweepTest`.
- Money: BigDecimal throughout (precision 12, scale 2); server-generated
  invoice numbers; `ManualPaymentProvider` = recording, not processing —
  correctly labelled in code.
- RBAC permission checks, security headers/CORS, error envelope
  (`ApiErrorSchema` on both webs), loading/empty/error/retry states —
  present and consistent.
- docker-compose.yml and per-product Render blueprints coherent.
- Session restore: both auth contexts resolve every path (no stored token /
  refresh success / refresh failure) into a terminal state; `initialized`
  gates protected UI.

## 4. Baselines (2026-10-09)

- platform-common: 7 tests PASS.
- localboost-api: 229 tests PASS.
- consulting-api: 136 baseline PASS; 148 PASS after P0-2.
- fixna-web: Vitest contact-handler suite (see change log for results).

## 5. Fix order (approved plan — status)

1. Baselines — DONE.
2. P0-1: .gitignore hardening — DONE.
3. P0-2: Consulting login rate limiting + tests — DONE.
4. P0-3: LocalBoost HealthProbe UX — DONE.
5. P1-2: contact Sheets flow + Vitest — DONE (owner setup pending).
6. P1-1/P2-1/P2-5/P2-6/P2-7: consistency + SEO + accuracy — DONE.
7. P1-3: CI for fixna-web — DONE (first push validates).
8. P1-5/P2-3/P2-4: runtime re-verification + deployment env — OWNER/OPEN.
9. Final verification pass + ecosystem-change-log.md — see change log.
