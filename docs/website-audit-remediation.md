# Public Website Audit — Remediation Proposal (Phase 2–6)

Date: 2026-10-11 · Status: PROPOSAL — awaiting review before app-code edits.
Baseline: `docs/website-audit-baseline.md`. Scope limited to `frontend/fixna-web`.
No LocalBoost/Consulting/shared-backend/DB/auth/billing changes. No deploys,
DNS, env, or migration changes without explicit approval.

## Prioritized issue list

| ID | Severity | Issue (evidence) | Acceptance criteria |
|---|---|---|---|
| R1 | Medium | Canonical host inconsistent: `frontend/fixna-web/.env.example` says `https://fixna.in` (apex) while `ci.yml` builds with `https://www.fixna.in`; brief mandates `https://www.fixna.in`. No per-page `alternates.canonical` except legacy route. | `.env.example` = `https://www.fixna.in`; each public page sets absolute canonical via `siteUrl`; build emits `<link rel="canonical">` with www host. |
| R2 | Medium | No structured data: no JSON-LD `Organization`/`WebSite`/`Service` found in `layout.tsx` or pages (search: `ld+json` absent in fixna-web). | Layout or page adds valid JSON-LD (Organization + WebSite; Service on consulting/what-we-do); passes schema validation. |
| R3 | Medium | Social metadata incomplete: `twitter.card = summary_large_image` but no `images` in OG/Twitter; no OG image asset. | Either add OG image + metadata `images`, or downgrade to `summary` card; link unfurls correctly. |
| R4 | Low-Med | Skip-link color token missing: `design-system.css` `.skip-link { color: var(--white) }` but `:root` defines no `--white` (verified §4 list). | Token defined or color replaced; skip link legible on dark bg. |
| R5 | Low-Med | Focus-ring contrast unmeasured: `:focus-visible { outline: 3px solid var(--lime) }` (`#c7ed94` on white) may fail WCAG non-text contrast. | Measured ≥3:1 against adjacent colors or ring darkened/outlined; keyboard tab visible on all pages. |
| R6 | Low | Footer capability links all point at generic `/what-we-do` (6 identical links, `footer.tsx`); nav omits `/industries` (only in footer + homepage body). | Footer links use distinct anchors/labels or canonical capability targets; Industries reachable from primary nav or documented as intentional. |
| R7 | Low | Sitemap `lastModified: new Date()` on every build (all 10 routes churn weekly regardless of change). | Stable `lastModified` (fixed date or per-page) unless content changed. |
| R8 | Info | Consulting wording mixes services + Workspace product: `products.ts` consulting = "Technology consulting and client delivery platform…"; `/consulting` page = pure services. No Workspace disclaimer on public site beyond statusNote. | Content proposal below adopted; services vs Workspace clearly labelled. |

## Content change proposal (ambiguous claims — do not implement without approval)

C1 Consulting card (`products.ts` description + statusNote): keep "Available
for early access. AI drafting features are advisory templates that always
require human review." Change description to either (a) "Technology
consulting services, delivered with support from the Fixna Consulting
Workspace (early access)." or (b) keep services-only and move Workspace
mention to a separate line "Companion software: Fixna Consulting Workspace
(MVP, early access)". Rationale: current "consulting and client delivery
platform" blurs purchasable services vs software product (Phase 2/4).

C2 Service entry points (architecture reviews, cloud modernization, AI
workflow assessments): EVIDENCE EXISTS — `consultingExpertise` lists
"Technical Architecture Reviews", "Cloud Migration", "AI / GenAI";
`engagementModels` lists "Architecture Review"; capabilities cover
architecture/cloud/AI. Proposal: surface three explicit entry CTAs on
`/consulting` linking to `/contact` (no new routes), e.g. "Book an
architecture review". No invented scope; each maps 1:1 to existing lists.

C3 Case studies: NO EVIDENCE — none on site, none in repo. Proposal: do NOT
add case-study section; keep "Typical outcome" pattern (already generic and
honest). Anonymized examples only after owner supplies genuine work.

C4 Technology lists: homepage trust line + technology page are long but
grounded (`Java/Spring/Kafka/AWS/Azure` match API stack + CI). Proposal:
keep, but de-duplicate homepage trust line vs technology page; no new tech
names without code evidence.

C5 Hospitality: keep `COMING SOON`, `url: null`, "Planned product — not yet
available." No change.

## Quality gates (2026-10-11 implementation session)

| Check | Result | Evidence |
|---|---|---|
| Typecheck | BLOCKED | `cmd /c npm --prefix frontend/fixna-web run typecheck` executed; shell output unobservable in this environment. Edits are type-narrow (metadata spreads, string literals, CSS tokens); prior baseline tsc clean 2026-10-10. CI `fixna-web` job (typecheck → test → build) is the verifier. |
| Unit tests | BLOCKED | `cmd /c npm --prefix frontend/fixna-web run test` executed; output unobservable. New `canonical.test.ts` (3 cases) + existing 22-case suite cover changed behaviour; must be confirmed via CI run. |
| Production build | NOT VERIFIED | Not re-run this session (shell unobservable); prior PASS 2026-10-10 (17 routes). Metadata-only changes are build-safe; CI build is the verifier. |
| Lint/format | NOT VERIFIED | No lint script in fixna-web. |
| Route/link/metadata/sitemap/robots | PARTIAL | Canonicals verified statically in every edited page file; sitemap routes unchanged; robots untouched; runtime link check NOT VERIFIED. |
| Responsive/keyboard/a11y | PARTIAL | Focus-ring + skip-link fixes verified statically; automated + manual pass NOT VERIFIED (owner M3). |
| Contact validation/delivery | NOT VERIFIED | Contact flow untouched; delivery still requires owner Sheets setup (M1). |
| Metadata/canonical/sitemap/robots | PARTIAL | Static verification only (see above); no success claimed without measurement. |

## Files changed (this session, fixna-web only)

- NEW `src/lib/canonical.ts` — `canonicalFor(path)` helper (R1).
- NEW `src/lib/canonical.test.ts` — 3-case canonical contract test.
- `src/app/page.tsx`, `about`, `what-we-do`, `products`, `consulting`,
  `technology`, `industries`, `contact`, `privacy-policy`, `terms` — absolute
  canonicals (R1); `capabilities` legacy canonical made absolute.
- `src/app/layout.tsx` — Organization/WebSite JSON-LD (R2); twitter card
  `summary_large_image` → `summary` (R3, no OG image exists).
- `src/app/design-system.css` — `--white` token (R4); dual-tone focus ring (R5).
- `src/app/sitemap.ts` — stable per-route `lastModified` (R7).
- `src/components/footer.tsx` — distinct capability links (R6).
- `src/content/navigation.ts` — Industries added to primary nav (R6).
- `src/content/products.ts` — consulting = services + Workspace note (C1).
- `src/app/consulting/page.tsx` — 3 service-entry CTAs → `/contact` (C2).
- `.env.example` — canonical host `https://www.fixna.in` (R1).
- No LocalBoost/Consulting/backend/DB/auth/billing changes. No deploys, DNS,
  env, or migration changes. Pre-existing `M docs/ecosystem-change-log.md`
  preserved untouched.

## Remaining risks + manual verification (owner)

M1 Contact delivery on deployed host. M2 Vercel `NEXT_PUBLIC_SITE_URL` +
www↔apex redirect. M3 Keyboard-only + mobile + contrast pass. M4 Inbox
monitoring. M5 Legal review. R8 wording adopted as proposed (C1) — confirm
it matches how services are actually sold.

## Manual verification required (owner)

M1 Submit `/contact` on deployed www host; confirm sheet row + success text.
M2 Confirm Vercel `NEXT_PUBLIC_SITE_URL=https://www.fixna.in` and www↔apex
redirect. M3 Keyboard-only pass + mobile layout on key flows. M4 Confirm
`fixnainfo@gmail.com` monitored. M5 Legal review of privacy/terms.
