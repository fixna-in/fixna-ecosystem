# Frontend Modernization & Optimization Plan
Modern, professional, shared design across every Fixna website

- **Date:** 2026-10-15
- **Scope:** All web apps in `frontend/`
  - `fixna-web` (corporate/marketing)
  - `localboost-web`
  - `consulting-web`
  - `hospitality-web`
  - `invoice-assistant-web`
  - `website-audit-web`
- **Common design root:** `packages/fixna-design-system` (extend with `packages/fixna-ui` if shared React primitives are needed)
- **Goal:** One professional visual system, one component vocabulary, better performance, accessibility, SEO, and maintainability across every website.

---

## 1. Objectives and success metrics

### Objectives
1. Make every Fixna website look and behave like one cohesive, modern, professional product family.
2. Keep all reusable design decisions in a **common folder/package**, not duplicated per app.
3. Optimize for Core Web Vitals, accessibility, SEO, responsiveness, and conversion.
4. Reduce duplicated CSS/component logic and prevent drift.
5. Establish verification and governance so quality stays high after launch.

### Success metrics
| Metric | Target |
|---|---:|
| Lighthouse mobile Performance | >= 90 |
| Lighthouse Accessibility | >= 95 |
| Lighthouse Best Practices | >= 95 |
| Lighthouse SEO | >= 95 |
| LCP | < 2.5s |
| INP | < 200ms |
| CLS | < 0.1 |
| Typecheck | 0 errors per app |
| Unit/e2e | green in CI |
| Shared-token violations | 0 raw palette values in app CSS |
| Theme flash | none on initial load |

---

## 2. Current state audit

### App matrix
| App | Stack | Styling approach | DS adoption | Main gaps |
|---|---|---|---|---|
| `fixna-web` | Next.js 16, React 19 | Plain CSS + design-system.css aliases | Partial: tokens + ThemeProvider; not base.css | Metadata/structured data incomplete, font loading strategy missing, shared shell not reusable |
| `localboost-web` | Next.js 16, React 19 | Plain CSS, large globals/product/workspace/responsive CSS | Tokens + base + ThemeProvider | Large duplicated CSS, mobile layout debt, asset hygiene |
| `consulting-web` | Next.js 16, React 19 | Plain CSS, very large globals.css | Tokens + base + ThemeProvider | Giant stylesheet, duplicated shell/form/card styles, content and SEO gaps |
| `hospitality-web` | Next.js 16, React 19 | Tailwind v4 bridge | Tokens + tailwind bridge + ThemeProvider | Placeholder UI, duplicated header, minimal metadata, no next image config |
| `invoice-assistant-web` | Next.js 16, React 19 | Tailwind v4 bridge | Tokens + tailwind bridge + ThemeProvider | Form/UI inconsistency, inline status, no metadata/icon assets config |
| `website-audit-web` | Next.js 16, React 19 | Tailwind v4 bridge | Tokens + tailwind bridge + ThemeProvider | Missing next.config.js, duplicated header, limited empty/error states |

### Shared infrastructure already available
- `packages/fixna-design-system`
  - `tokens.css`
  - `base.css`
  - `tailwind-theme.css`
  - `ThemeProvider`, `ThemeToggle`
  - `theme.ts` with no-flash init script
- `packages/fixna-brand`
  - `BrandMark`, `BRAND_MARK_SVG`

### Key findings
1. **Design system exists, but adoption is uneven.**
   - Plain-CSS apps import tokens/base.
   - Tailwind apps use the bridge, but they still inline shells and content patterns.
2. **Shared React components are minimal.**
   - Only `ThemeProvider`, `ThemeToggle`, and `BrandMark` are shared.
   - Product headers, shells, cards, alerts, footer, skeletons, empty states, and SEO helpers are duplicated.
3. **CSS duplication remains high.**
   - `consulting-web/src/app/globals.css` and `localboost-web/src/app/*.css` contain large repeated layout/form/card CSS.
4. **Performance optimization is incomplete.**
   - No app uses `next/font` visibly.
   - No shared image optimization policy.
   - Lighthouse/Playwright quality gates are not part of normal CI for all apps.
5. **SEO is strongest on `fixna-web`, weaker elsewhere.**
   - Product web apps need consistent metadata, structured data, canonical URLs, and sitemaps.
6. **Accessibility exists but needs formal gates.**
   - Focus rings, skip links, landmarks, and reduced-motion are partially present.
   - Contrast, screen-reader flow, and keyboard ergonomics should be tested automatically.

---

## 3. Common design strategy

### Rule: put design in the common package, not in each website
The common root is:

```text
packages/
  fixna-design-system/   # tokens, themes, primitives, runtime, base styles
  fixna-brand/           # brand mark and brand assets
  fixna-ui/              # optional shared React components (phase 2)
```

Apps must:
- Import tokens/theme runtime from the common package.
- Use shared primitives instead of duplicating base CSS.
- Use shared React components for shells, headers, footers, cards, alerts, etc.
- Never hardcode palette colors or create app-local token systems.
- Keep only product-specific layout/composition CSS in app folders.

### Why `packages/` instead of `frontend/common`
The repo already uses `packages/*` as the monorepo common layer. Keeping design there allows versioned builds, workspace resolution, and consistent sharing with future packages. A `frontend/common` folder would create a weaker, ad-hoc dependency boundary.

### Common design contract
Every frontend app must follow this import order:

**Plain-CSS apps (`fixna-web`, `localboost-web`, `consulting-web`)**
```tsx
import { ThemeProvider } from "@fixna/design-system";
import { themeInitScript } from "@fixna/design-system/theme";
import "@fixna/design-system/tokens.css";
import "@fixna/design-system/base.css";
import "./globals.css";
```

**Tailwind apps (`hospitality-web`, `invoice-assistant-web`, `website-audit-web`)**
```css
@import "@fixna/design-system/tokens.css";
@import "@fixna/design-system/tailwind-theme.css";
@import "tailwindcss";
```

Plus `<ThemeProvider>` and no-flash script in `layout.tsx`.

---

## 4. Target architecture

### Layer 1 — Design tokens and themes
`packages/fixna-design-system`
- Semantic color tokens
- Font, spacing, radius, shadow, motion tokens
- Light/dark/system theme behavior
- Tailwind bridge
- Base CSS primitives

### Layer 2 — Shared UI primitives
Add a reusable component layer, either in `fixna-design-system/src/components` or in a new `packages/fixna-ui`.

Recommended shared components:
- `BrandHeader`
- `MarketingHeader`
- `AppSidebar`
- `AppShell`
- `PageShell`
- `Footer`
- `Section`
- `Card`
- `EmptyState`
- `ErrorAlert`
- `LoadingSkeleton`
- `FormField`
- `Button` and button variants
- `Badge`
- `Tabs`
- `DataTable`
- `ThemeToggle` (already exists)

### Layer 3 — Product apps
Each app keeps only:
- Route/page composition
- Product-specific sections
- Product-specific form state
- App business logic

### Layer 4 — Verification
- Typecheck
- Unit tests
- E2E smoke tests
- Accessibility scans
- Visual regression
- Lighthouse CI
- Bundle/performance budgets

---

## 5. Phased implementation plan

### Phase 0 — Baseline and tooling (1-2 days)
**Goal:** Establish measurable starting points.

Tasks:
- Run and record for every app:
  - `npm run typecheck` where available
  - `npm run test`
  - `npm run build`
  - Lighthouse mobile/desktop
  - axe accessibility scan
- Capture baseline screenshots at `390px`, `768px`, `1280px`, light/dark.
- Add `frontend/README.md` documenting common design import contract.

Exit criteria:
- Baseline report stored in `docs/baselines/`.
- Known issue list is complete.

### Phase 1 — Strengthen the common design system (2-3 days)
**Goal:** Make the common package complete enough for every app.

Tasks:
- Audit `tokens.css` for missing semantic states:
  - hover/focus/active/muted variants
  - elevated surface and overlay clarity
  - brand-aligned success/warning/danger/info scale
- Expand `base.css` plain-CSS primitives so apps stop duplicating:
  - public header/footer
  - app shell/sidebar
  - cards/panels
  - data tables
  - empty/loading/error states
  - skip link
- Expand Tailwind bridge equivalents.
- Add Storybook or a simple `/design-system` preview route in `fixna-web`.
- Document every token and component in `packages/fixna-design-system/README.md`.

Exit criteria:
- New app can be built from primitives with minimal custom CSS.
- Tokens and primitives are documented.

### Phase 2 — Shared component layer (3-5 days)
**Goal:** Move reusable React UI into common package.

Tasks:
- Create `packages/fixna-ui` or add components to design system.
- Export common components with typed props.
- Add tests for rendering/theme behavior.
- Migrate one app first (`hospitality-web`) as the reference.
- Avoid business logic in common components.

Exit criteria:
- At least `PageShell`, `MarketingHeader`, `Footer`, `Card`, `ErrorAlert`, `EmptyState` are shared.
- New components are typed and tested.

### Phase 3 — Plain-CSS apps modernization (4-6 days)

#### `fixna-web`
Tasks:
- Strengthen metadata and structured data:
  - JSON-LD Organization/WebSite
  - OG image
  - canonical consistency
  - sitemap and robots verification
- Add `next/font` for Inter.
- Replace custom aliases with direct semantic tokens where practical.
- Improve image handling and lazy loading.
- Standardize section spacing and heading scale.
- Add product/status honesty content refinements from existing audit.

#### `localboost-web`
Tasks:
- Shard and reduce `globals.css`, `product.css`, `workspace.css`, `responsive.css`.
- Move shell/layout/form/button/card styles into common base CSS.
- Keep only app-specific dashboard/campaign styles.
- Improve mobile navigation and table responsiveness.
- Verify empty/loading/error states across all routes.
- Add stable selectors for E2E tests.

#### `consulting-web`
Tasks:
- Split huge `globals.css` into:
  - app shell imports from common
  - dashboard components
  - public website rendering
  - form/table styles from common
- Move duplicated form, table, card, and alert styles into shared base.
- Improve public site rendering accessibility.
- Add metadata and OG consistency.

Exit criteria:
- CSS file sizes noticeably reduced.
- No duplicated base component styles.
- All routes remain functional.

### Phase 4 — Tailwind apps modernization (3-4 days)
Apps: `hospitality-web`, `invoice-assistant-web`, `website-audit-web`.

Tasks:
- Replace inline headers/footers with shared components.
- Create consistent dashboards:
  - page header
  - toolbar/filter row
  - card grid
  - empty state
  - loading skeleton
  - error banner
- Standardize forms with shared `FormField` and validation display.
- Add metadata, icons, manifest where missing.
- Add `next.config.js` consistency (website-audit currently lacks one).
- Configure image optimization.

Exit criteria:
- All three Tailwind apps use the same shell patterns.
- Theme toggle behavior and styling are consistent.
- Production builds pass.

### Phase 5 — Performance optimization (2-3 days)
Tasks:
- Use `next/font` for Inter in all apps.
- Preload critical assets only.
- Use `next/image` for raster images; inline or optimize SVG.
- Remove unused CSS and JavaScript.
- Split heavy client components and use server components where possible.
- Add caching headers for static assets.
- Enable compression in deployment.
- Use `React.memo`, virtualization, or pagination for large lists if needed.
- Add bundle analyzer during optimization.

Targets:
- Main route JS reduced by at least 15% where feasible.
- LCP/CLS/INP within targets.
- No layout shift from fonts.

### Phase 6 — SEO and content modernization (2-4 days)
Tasks:
- Standardize metadata via templates:
  - title: `%s | Fixna`
  - description
  - canonical
  - Open Graph
  - Twitter card
- Add OG images per product.
- Add JSON-LD where useful:
  - Organization
  - WebSite
  - SoftwareApplication/Product
  - ContactPage
- Clean sitemap and robots.
- Resolve canonical domain consistency (`www.fixna.in` vs apex).
- Keep product status honesty:
  - mock adapters
  - advisory AI
  - roadmap products

Exit criteria:
- Every public page has unique metadata.
- Structured data validates.
- Sitemap is consistent with route inventory.

### Phase 7 — Accessibility and quality (2-3 days)
Tasks:
- Add Playwright + axe tests for representative pages.
- Verify:
  - keyboard navigation
  - skip links
  - focus visibility
  - landmark structure
  - color contrast
  - reduced motion
  - form field validation announcements
  - screen-reader labels for icons
- Fix BrandMark accessibility.
- Ensure theme toggle accessible name reflects current mode.

Exit criteria:
- Automated accessibility scan has no critical/serious issues on sampled pages.
- Manual keyboard walkthrough passes.

### Phase 8 — Visual regression and CI gates (2-3 days)
Tasks:
- Add Playwright screenshot tests:
  - marketing pages
  - app shells
  - light/dark
  - mobile/tablet/desktop
- Add Lighthouse CI with minimum thresholds.
- Run build/typecheck/test for all apps in CI.
- Add performance budget files.
- Add PR checklist for design system changes.

Exit criteria:
- Every app has CI coverage.
- Regressions are detectable before merge.

---

## 6. File-level change plan

### Common package
- `packages/fixna-design-system/src/tokens.css`
- `packages/fixna-design-system/src/base.css`
- `packages/fixna-design-system/src/tailwind-theme.css`
- `packages/fixna-design-system/src/ThemeProvider.tsx`
- `packages/fixna-design-system/src/ThemeToggle.tsx`
- `packages/fixna-design-system/src/theme.ts`
- `packages/fixna-design-system/README.md`
- New: `packages/fixna-design-system/src/components/*`
- New optional: `packages/fixna-ui/*`

### `fixna-web`
- `src/app/layout.tsx`
- `src/app/design-system.css`
- `src/app/globals.css`
- `src/components/page-shell.tsx`
- `src/components/navigation.tsx`
- `src/components/footer.tsx`
- `src/app/page.tsx`
- `next.config.mjs`

### `localboost-web`
- `src/app/layout.tsx`
- `src/app/globals.css`
- `src/app/product.css`
- `src/app/workspace.css`
- `src/app/responsive.css`
- `src/components/app-shell.tsx`
- `src/components/ui.tsx`

### `consulting-web`
- `src/app/layout.tsx`
- `src/app/globals.css`
- `src/components/app-shell.tsx`
- public website pages under `src/app/*`
- public-facing routes: `/`, `/site/[slug]`

### `hospitality-web`
- `src/app/layout.tsx`
- `src/app/globals.css`
- `tailwind.config.js`
- `src/app/page.tsx`
- `next.config.js`

### `invoice-assistant-web`
- `src/app/layout.tsx`
- `src/app/globals.css`
- `src/app/page.tsx`
- `next.config.js`

### `website-audit-web`
- `src/app/layout.tsx`
- `src/app/globals.css`
- `src/app/page.tsx`
- New: `next.config.js`

---

## 7. Visual and UX modernization details

### Brand expression
- Keep brand dark green `#163e32` and lime `#c7ed94`.
- Use neutral surfaces, not green-tinted backgrounds.
- Reserve lime for accents, focus glow, small emphasis.
- Use Inter with clean spacing and negative letter-spacing on headings.
- Prefer calm shadows and subtle motion.

### Layout
- Consistent max widths:
  - marketing: `1140px` / `1440px`
  - app shell: sidebar + main grid
  - forms: `480px`
- Consistent card padding, border radius, and elevation.
- Consistent empty/loading/error states.

### Motion
- Page-level animations should be subtle and accessible.
- Respect `prefers-reduced-motion`.
- Avoid large parallax or continuous animation.
- Use 160-180ms transitions for hover/focus.

### Content
- Clear value proposition on each hero.
- Honest product status.
- No fabricated testimonials, clients, or metrics.
- Strong CTAs with obvious destination and intent.

---

## 8. Optimization checklist

### Next.js configuration
```js
const nextConfig = {
  reactStrictMode: true,
  images: {
    formats: ["image/avif", "image/webp"],
    deviceSizes: [640, 750, 828, 1080, 1200, 1920],
  },
  compress: true,
  poweredByHeader: false,
  experimental: {
    optimizePackageImports: ["@fixna/design-system", "@fixna/brand"],
  },
};
```

### Font optimization
- Load Inter using `next/font/google`.
- Apply font variable in root layout.
- Avoid CSS `@import` of remote fonts.
- Use `display: "swap"` with no layout shift.

### Rendering
- Use server components by default.
- Make only interactive paths client components.
- Split editor/upload/form components dynamically.
- Keep API clients lazy where practical.

### CSS
- Plain-CSS apps must consume base primitives.
- Keep app CSS focused on composition.
- Use Tailwind utility classes consistently in Tailwind apps.
- Remove dead selectors during refactor.

### Accessibility
- Every page has one `h1`.
- Landmark structure: `header`, `main#main-content`, `footer`.
- Keyboard reachable controls and visible focus.
- `alt` text or `aria-hidden` for decorative images/SVG.
- Form errors use `role="alert"` or `aria-live`.
- Respect reduced motion.

### SEO
- Metadata exported from every route.
- Consistent canonical base URL.
- JSON-LD on key pages.
- `sitemap.ts` and `robots.ts`.
- No duplicate routes without canonical handling.

---

## 9. Verification matrix

| Check | Command / method | Applies to |
|---|---|---|
| Typecheck | `npm run typecheck` or `npx tsc --noEmit` | all with config |
| Unit tests | `npm run test` / `npx vitest run` | apps with tests |
| Production build | `npm run build` | all |
| Lighthouse | Lighthouse CLI / CI | all |
| Accessibility | Playwright + axe | sampled routes |
| Visual regression | Playwright screenshots | sampled routes |
| Bundle analysis | `@next/bundle-analyzer` | all |

Representative routes for QA:
- `/`
- `/about`
- `/what-we-do`
- `/products`
- `/consulting`
- `/contact`
- `/privacy-policy`
- dashboard/home route for each product app
- login/register route for each product app
- dark and light mode
- mobile, tablet, desktop

---

## 10. Governance

### Design change process
1. Propose token/component change in `packages/fixna-design-system`.
2. Update package README.
3. Run design-system tests.
4. Migrate affected apps.
5. Update visual regression baselines intentionally.
6. Document change in changelog.

### Rules for all contributors
- Do not add raw palette colors in app CSS.
- Do not create local token files.
- Do not duplicate shared shell/header/footer/card styles.
- Put reusable design code in common package.
- Update tests and screenshots for visual changes.

### PR checklist
- [ ] Typecheck passes
- [ ] Tests pass
- [ ] Build passes
- [ ] Dark theme checked
- [ ] Mobile viewport checked
- [ ] Accessibility checked
- [ ] No raw palette values added
- [ ] Common package reused where possible

---

## 11. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Large CSS refactor causes regressions | Incremental migration, screenshots, E2E tests |
| Common components become too opinionated | Keep variants and composition flexible |
| Performance regressions from shared package | Bundle analysis and tree-shaking checks |
| Theme token changes affect all apps | Versioned tokens and migration notes |
| Tailwind/plain-CSS divergence | Keep both bridges in common package |
| SEO regressions | Metadata tests and canonical checks |

---

## 12. Recommended execution order

1. Phase 0: baseline and tooling
2. Phase 1: strengthen common design system
3. Phase 2: shared component layer
4. Phase 3: plain-CSS apps
5. Phase 4: Tailwind apps
6. Phase 5: performance
7. Phase 6: SEO/content
8. Phase 7: accessibility
9. Phase 8: visual regression and CI gates

This order protects product functionality while allowing design consistency and performance to improve incrementally.

---

## 13. Immediate next actions

1. Confirm this plan as the roadmap.
2. Start Phase 0 baseline capture for all six apps.
3. Extend `packages/fixna-design-system` with shared shell, header, footer, card, alert, and skeleton components.
4. Begin with `hospitality-web` as the reference Tailwind migration.
5. Then migrate `localboost-web` CSS deduplication because it has the largest repeated stylesheet surface.
6. Add CI gates as soon as Phase 2 is stable.

---

## 14. Notes on “common folder”
For this repository, the common design folder is already `packages/fixna-design-system`. Future shared UI components can live in:
- `packages/fixna-design-system/src/components` for token-driven primitives, or
- a new `packages/fixna-ui` for higher-level React components.

Both are valid, but the CSS/design tokens must remain in `packages/fixna-design-system` so there is exactly one source of truth for color, typography, spacing, theme, and base styling.