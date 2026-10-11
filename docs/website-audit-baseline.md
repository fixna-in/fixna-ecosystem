# Public Website Audit — Phase 1 Baseline (fixna.in)

Date: 2026-10-11 · Mode: read-only discovery; no application code edited.
Public site source: `frontend/fixna-web` (Next.js 16.4.0, React 19, TypeScript strict).
Scripts (`frontend/fixna-web/package.json`): `dev` (port 3002), `build`,
`start` (port 3002), `typecheck` (`tsc --noEmit`), `test` (`vitest run`).
Deps: `@fixna/brand`, `google-auth-library` (server-only, lazy import),
`next`, `react`, `react-dom`; dev: `typescript`, `vitest`, `@types/*`.

Git status at baseline: only `M docs/ecosystem-change-log.md` (pre-existing
user change, unrelated — preserved, untouched).

## 1. Route inventory (verified from disk)

| Route | File | Notes |
|---|---|---|
| `/` | `src/app/page.tsx` | Hero, capabilities, products, approach, industries, CTAs |
| `/about` | `src/app/about/page.tsx` | Beliefs, approach |
| `/capabilities` | `src/app/capabilities/page.tsx` | Legacy duplicate; `canonical: /what-we-do` + `noindex, follow` |
| `/what-we-do` | `src/app/what-we-do/page.tsx` | Canonical capabilities route (nav/footer/CTA target) |
| `/products` | `src/app/products/page.tsx` | 3 product cards with `statusNote` honesty lines |
| `/consulting` | `src/app/consulting/page.tsx` | Services, expertise, engagement models |
| `/technology` | `src/app/technology/page.tsx` | 7 technology groups |
| `/industries` | `src/app/industries/page.tsx` | 4 industries, future-products callout |
| `/contact` | `src/app/contact/page.tsx` + `contact-form.tsx` | `POST /api/contact` + mailto fallback |
| `/privacy-policy`, `/terms` | `src/app/*/page.tsx` | Legal; privacy discloses Sheets storage, no 3rd-party analytics |
| `POST /api/contact` | `src/app/api/contact/route.ts` + `handler.ts` + `sheets.ts` | Validation, honeypot, per-IP limit, Sheets append |

Shared: `PageShell` (skip link, `Header`, `main#main-content`, `Footer`),
`navigation.ts` (5 nav items; Industries absent from nav),
`product-links.ts` (localboost/consulting URLs, hospitality `null`),
`site-url.ts` (canonical base, localhost fallback), `sitemap.ts` (10 routes),
`robots.ts` (allow all + sitemap), root `layout.tsx` (title template,
OG/Twitter, icons, robots index/follow).

## 2. Baseline checks (this session)

| Check | Result | Evidence |
|---|---|---|
| Lint/format | NOT VERIFIED | No lint script in `fixna-web`; shell output unobservable this session |
| Typecheck | BLOCKED | `npm` blocked by PowerShell policy; `cmd /c` run unobservable |
| Unit tests | BLOCKED | Same; prior evidence: 22 Vitest pass 2026-10-11 (change log) |
| Production build | BLOCKED | Same; prior evidence: pass 2026-10-10 (17 routes) |
| Link/metadata/sitemap/robots | PARTIAL | Verified statically (see §4); runtime NOT VERIFIED |
| Responsive/keyboard/a11y | NOT VERIFIED | Static markup reviewed; no automated or manual run this session |
| Contact delivery | NOT VERIFIED | Requires Sheets env + redeploy (owner action, unchanged) |

## 3. Product-accuracy verification (source vs public claims)

- LocalBoost mock adapters: DISCLOSED — `products.ts` statusNote states
  "simulated (mock) adapters — no live ad spend occurs". Verified in code;
  consistent with `FIXNA_AI_PROVIDER`/`FIXNA_PLATFORM_MODE` mock defaults.
- Consulting AI advisory-only: DISCLOSED — statusNote requires human review;
  consistent with `MockAiProvider` in consulting-api.
- Hospitality coming soon: DISCLOSED — status `COMING SOON`, `url: null`,
  footer/nav "Coming Soon". No repo evidence of availability.
- Services vs Workspace distinction: PARTIAL — `/consulting` describes
  professional services; `products.ts` consulting blurb mixes "consulting
  and client delivery platform". Needs a content proposal (Phase 2).
- No testimonials, client names, metrics, or certifications found on the
  public site. No fabricated claims detected.

## 4. SEO / accessibility static review

Present: unique titles/descriptions per page; canonical + noindex on legacy
route; OG/Twitter/siteName/locale; sitemap (10 content routes, excludes
intentionally-noindexed `/capabilities`); robots allow-all + sitemap ref;
skip link; landmarks (`header`/`main#main-content`/`footer`); labelled form
fields with `aria-invalid` + `role=alert` errors; submitting/success/error
states; `aria-expanded` mobile menu; `prefers-reduced-motion` rule.
Gaps/hypotheses: no JSON-LD structured data; sitemap omits `/capabilities`
(deliberate, acceptable); `.env.example` canonical is `https://fixna.in`
(apex) while CI builds with `https://www.fixna.in` — inconsistent;
`NEXT_PUBLIC_SITE_URL` unset falls back to localhost URLs; footer capability
links all point at `/what-we-do` (no deep anchors); lime `#c7ed94` focus
ring on white may fail contrast (unmeasured); `BrandMark` SVG a11y attrs
unverified; no `vercel.json`/redirect config for fixna-web found in repo.

## 5. Contact form (current mechanism, verified)

`contact-form.tsx` POSTs JSON to `/api/contact`. `handler.ts`: trims +
validates (lengths, email/phone shapes), honeypot trap
`fx_hp_confirm_9q2` (202 fake-success, stores nothing), in-memory per-IP
limit (default 3/10 min), 405/400/429/502/503 mapping, "recorded" wording
(never "emailed"). `sheets.ts`: service-account Sheets append, secrets via
`GOOGLE_SERVICE_ACCOUNT_JSON` + `GOOGLE_SHEET_ID` (server only). Email
fallback `fixnainfo@gmail.com` visible on page, form, errors, privacy
policy. Client keeps input on failure, `form.reset()` on success.
Suite `handler.test.ts` (17 cases) + `site-url.test.ts` (4 cases).

## 6. Unknowns (need runtime/owner input)

U1 Live contact delivery (Sheets env set? rows landing?). U2 Deployed
`NEXT_PUBLIC_SITE_URL` value on Vercel. U3 Canonical host www vs apex +
redirect behavior. U4 Vercel project/root mapping for fixna-web. U5 Live
header/footer rendering, contrast, keyboard flow (manual). U6 Whether
`fixnainfo@gmail.com` inbox is monitored. U7 Legal review of privacy/terms.
