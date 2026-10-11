# Ecosystem Change Log — 2026-10-09 (Act-mode audit pass)

All changes are additive or surgical; no migrations, no API-contract breaks,
no production deploys. Uncommitted user work (`.gitignore` hardening + Stage-1
docs) preserved and extended.

## Backend

### consulting-api — P0-2 auth rate limiting (LocalBoost parity)

- NEW `in/fixna/platform/common/web/RateLimiter.java` — sliding-window
  limiter (ported from localboost-api; promote to platform-common when a
  third consumer appears).
- NEW `in/fixna/platform/common/web/RateLimitFilter.java` — 429 +
  `Retry-After` + standard envelope on `/api/v1/auth/**` AND
  `/api/v1/portal/auth/**` (Consulting has a portal login LocalBoost lacks).
- `SecurityConfig` — filter wired after the JWT filter.
- `application.yml` — `fixna.security.rate-limit.per-ip-per-minute:
  ${FIXNA_RATE_LIMIT_PER_IP_PER_MINUTE:20}`.
- NEW tests: `RateLimiterTest` (5), `RateLimitFilterTest` (6, incl. portal
  path + concurrency). Suite result: **148 tests, 0 failures** (was 136).

### localboost-api

- No source changes. Baseline suite re-verified: **229 tests, 0 failures**.

## Frontend

### fixna-web (corporate site)

- NEW contact submission flow (P1-2), replacing the `mailto:` form action:
  - `src/app/api/contact/route.ts` — `POST /api/contact` Next.js route
    handler (deploys as a serverless function on Vercel automatically).
  - `src/app/api/contact/handler.ts` — pure, dependency-injected logic:
    validation (lengths, email/phone shapes), honeypot bot trap, per-IP
    fixed-window rate limit (default 3 / 10 min), 405/400/429/502/503
    mapping, success message that says "recorded" (never "emailed").
  - `src/app/api/contact/sheets.ts` — Google Sheets append via service
    account (`GOOGLE_SERVICE_ACCOUNT_JSON` + `GOOGLE_SHEET_ID`), lazy
    `google-auth-library` import, status-only error logging.
  - `src/app/contact/contact-form.tsx` — client form with submitting /
    success / field-error states, preserves input on failure, honeypot
    input, mailto fallback link.
  - `contact/page.tsx`, `design-system.css` updated (`.field-error`,
    `.hp-field`, `.contact-success`, `.muted-text`).
  - `.env.example` extended with contact vars + setup notes.
  - Contact honeypot false-positive fixed (found in production
    verification): browsers/password managers were autofilling the
    `companyWebsite` trap with a stored URL, so real enquiries returned
    202 but were discarded without reaching Sheets. Trap field renamed to
    the obscure shared constant `CONTACT_HONEYPOT_FIELD`
    (`fx_hp_confirm_9q2`), value read from FormData at submit time,
    `autoComplete="new-password"` + `spellCheck={false}`; server logs
    `contact:honeypot trap` on traps (no PII) for future diagnosability.
    Legacy `companyWebsite` values are now ignored as plain fields.
- NEW Vitest harness (`vitest` devDependency, `npm test` script):
  - `src/app/api/contact/handler.test.ts` — 18 cases (incl. honeypot
    trap + legacy `companyWebsite` regression).
  - `src/lib/site-url.test.ts` — 4 cases (converted from a stray
    node:test `.mjs` file that broke `vitest run`).
  - Result: **22 tests, 2 files, all passing**.
- Contact email standardized on `fixnainfo@gmail.com` across the
  contact page, contact-form fallback links/messages, API handler
  fallback messages, and privacy-policy contact line; zero
  `hello@fixna.in` references remain (fixna-web test + typecheck
  re-verified after the swap: 21 pass, tsc clean).
- SEO/navigation (P1-1, P2-6, P2-7):
  - Nav + footer nav now link to `/what-we-do`; `/capabilities` stays live
    with `canonical=/what-we-do` + `noindex`.
  - `sitemap.ts` lists all 10 public content routes.
  - `products.ts` — `statusNote` per product (mock ad adapters; advisory
    AI; planned); rendered on homepage + products page; trailing-slash
    consistency.
  - Homepage status badge class logic aligned with products page.
- Privacy policy now describes real behavior (Sheets storage, no
  third-party analytics scripts, Vercel logs). Not legal advice — flag for
  professional review.

### localboost-web

- `src/app/providers.tsx` — `HealthProbe` (landing page) degrades to a
  neutral `role="status"` note instead of a raw error banner when the API
  is unreachable (P0-3). Production build re-verified via `build:all`.

## Repo / CI

- `.gitignore` — secret-pattern hardening (`git`, `*.pem`, `*.key`,
  `*-key.json`, `*service-account*.json`, `google-credentials*.json`,
  `secrets/`, `credentials/`) + global `target/` (P0-1, P2-2).
- Root `package.json` — `build:all` now includes `build:fixna`.
- `frontend/localboost-web/package.json` — `test` script corrected from
  watch-mode `vitest` to one-shot `vitest run` (CI-safe default; no test
  files exist in that app yet).
- `.github/workflows/ci.yml` — NEW `fixna-web` job (typecheck → vitest →
  production build with `NEXT_PUBLIC_SITE_URL=https://www.fixna.in`).
- `package-lock.json` — updated by `npm install` (google-auth-library,
  vitest + transitive deps).

## Tests executed (this pass)

| Suite | Result |
|---|---|
| platform-common | 7 pass (baseline) |
| localboost-api | 229 pass (baseline) |
| consulting-api | 148 pass (136 baseline + 12 new; 0 failures) |
| fixna-web vitest | 22 pass (2 files) — executed 2026-10-11 (honeypot fix verified) |
| fixna-web typecheck | pass (tsc --noEmit clean) — executed 2026-10-10 |
| fixna-web production build | pass — 17 routes incl. dynamic /api/contact — executed 2026-10-10 |
| localboost-web production build | pass — 11 routes — executed 2026-10-10 |
| consulting-web production build | pass — 15 routes incl. /site/[slug] — executed 2026-10-10 |

## Deployment steps (owner action required)

1. **Contact form (one-time, ~15 min):**
   1. In the `fixnainfo@gmail.com` Google account, create a spreadsheet
      (e.g. "Fixna enquiries") with header row:
      `Received at, Name, Company, Email, Phone, Requirement`.
   2. In Google Cloud (same account is fine): create a service account,
      enable the **Sheets API**, create a JSON key.
   3. Share the spreadsheet with the service account email (Editor).
   4. In the Vercel project for fixna.in, set:
      - `GOOGLE_SERVICE_ACCOUNT_JSON` = full JSON key content (single line)
      - `GOOGLE_SHEET_ID` = spreadsheet ID (from its URL)
      - optionally `NEXT_PUBLIC_SITE_URL=https://www.fixna.in`
        (required for correct canonical/OG/sitemap URLs — P2-3)
   5. Redeploy. Verify: submit the form on /contact, confirm a new sheet
      row, confirm the success message; retry within the window to confirm
      429; submit with a bad email to confirm inline validation.
2. **Consulting API (Render):** set optional
   `FIXNA_RATE_LIMIT_PER_IP_PER_MINUTE` (defaults to 20/min/IP); redeploy
   the API image. Verify: repeated `/api/v1/auth/login` calls return 429
   with `Retry-After` after the quota.
3. **Key rotation (P0-1):** if the stray `git` key file was ever pushed,
   revoke/rotate it on GitHub; the file is now git-ignored and must never
   be recommitted.

## Known limitations

- Contact-form rate limiting is per serverless instance (in-memory); a
  shared store (Redis/KV) is the upgrade path if abuse becomes a problem.
- Google Sheets is an MVP enquiry store, not a CRM; export/retention
  process is a business decision.
- `api.fixna.in` health (audit P1-5) and `app.fixna.in` Vercel mapping
  (P2-4) need runtime verification from the deployed environment.
- Legal review of privacy policy/terms is out of scope (flagged, not
  invented).
- Pre-existing `npm audit` findings (8: 3 moderate, 3 high, 2 critical)
  predate this pass — mostly esbuild/vite/vitest dev-only chains in the
  product webs plus next 16.3.6.
- 2026-10-10 dependency pass: `npm audit fix` upgraded next 16.3.6 → 16.4.0
  in all three webs; **8 → 3 findings** (remaining 3: vite/esbuild chains in
  devDependencies only, not shipped to production). Fix was compatibility-only
  — no code changes required; all three webs build clean.
- Full `npm audit fix --force` (major bumps) deliberately NOT applied —
  breaking-change risk, needs its own task.
