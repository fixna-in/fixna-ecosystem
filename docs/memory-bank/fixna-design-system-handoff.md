# Fixna Design System — Styles & Theme Handoff (no secrets)

**Purpose:** Paste into ChatGPT or another assistant for full styling context across all Fixna products.  
**Last updated:** 2026-10-13  
**Domain:** fixna.in  
**Scope:** Shared design system consumed by fixna-web, localboost-web, consulting-web, hospitality-web, website-audit-web, invoice-assistant-web

---

## 1. Architecture

```
packages/fixna-design-system/
├── src/
│   ├── tokens.css          # Semantic color tokens, font stack, spacing, radii (SINGLE SOURCE OF TRUTH)
│   ├── base.css            # Plain-CSS component primitives (buttons, cards, forms, tables, badges)
│   ├── tailwind-theme.css  # Tailwind v4 theme bridge (maps tokens → @theme colors, custom dark: variant)
│   ├── theme.ts            # Theme preference logic (light/dark/system, localStorage persistence)
│   ├── ThemeProvider.tsx   # React context wrapper that applies data-theme to <html>
│   ├── ThemeToggle.tsx     # React toggle component
│   └── theme.test.ts       # Unit tests for theme preference logic (18 tests, all passing)
```

**Import pattern in product apps:**
```bash
import "@fixna/design-system/tokens.css";
import "@fixna/design-system/base.css";       # plain-CSS apps
@import "@fixna/design-system/tailwind-theme.css";  # Tailwind apps
```

**Theme switching:** `<ThemeProvider>` sets `data-theme="dark"` or `data-theme="light"` on `<html>`. The `themeInitScript` runs before paint to prevent flash. System preference works via `@media (prefers-color-scheme: dark)` when no explicit theme is set.

---

## 2. Color palette (neutral, professional — matching aiwithhassan.com)

### Light theme (`color-scheme: light`)

| Token | Value | Usage |
|-------|-------|-------|
| `--background` | `#ffffff` | Page background |
| `--foreground` | `#111827` | Primary text |
| `--surface` | `#ffffff` | Cards, panels |
| `--surface-elevated` | `#ffffff` | Popovers, dropdowns |
| `--surface-muted` | `#f4f4f4` | Table headers, subtle fills |
| `--primary` | `#163e32` | Brand dark green — actions, nav |
| `--primary-foreground` | `#ffffff` | Text on primary |
| `--primary-hover` | `#0e2b24` | Primary hover state |
| `--accent` | `#c7ed94` | Brand lime — highlights, selection |
| `--accent-foreground` | `#14342a` | Text on accent |
| `--secondary` | `#f3f3f3` | Secondary buttons |
| `--muted` | `#f0f0f0` | Muted backgrounds |
| `--muted-foreground` | `#6b7280` | Secondary text |
| `--border` | `#e5e7eb` | Borders, dividers |
| `--input` | `#ffffff` | Form input background |
| `--ring` | `#2563eb` | Focus ring (blue) |
| `--link` | `#163e32` | Link color |

### Dark theme (`color-scheme: dark`)

| Token | Value | Usage |
|-------|-------|-------|
| `--background` | `#171a18` | Page background (soft charcoal, not pure black) |
| `--surface-hero` | `#0f1110` | Deeper than bg for hero/CTA emphasis |
| `--foreground` | `#f3f4f6` | Primary text (clean light gray) |
| `--surface` | `#1f2421` | Cards, panels (visible separation from bg) |
| `--surface-elevated` | `#262c28` | Popovers, dropdowns |
| `--surface-muted` | `#2b312d` | Table headers, subtle fills |
| `--primary` | `#2a7a63` | Brand green (brightened for dark) |
| `--primary-foreground` | `#ffffff` | Text on primary |
| `--primary-hover` | `#349077` | Primary hover |
| `--accent` | `#c7ed94` | Brand lime (same as light) |
| `--accent-foreground` | `#000000` | Text on accent (black for contrast) |
| `--secondary` | `#262c28` | Secondary surfaces |
| `--muted` | `#262c28` | Muted backgrounds |
| `--muted-foreground` | `#a1a1aa` | Secondary text |
| `--border` | `#343b36` | Borders, dividers |
| `--input` | `#1f2421` | Form input background |
| `--ring` | `#60a5fa` | Focus ring (light blue) |
| `--link` | `#86efac` | Link color (light green) |

### Status colors

| Token | Light | Dark |
|-------|-------|------|
| `--success` | `#1f7a3d` | `#5fd39a` |
| `--success-soft` | `#e3f4e7` | `#10281d` |
| `--warning` | `#8a5a00` | `#e6b53f` |
| `--warning-soft` | `#fbf0d5` | `#2a2210` |
| `--danger` | `#b42318` | `#f2897a` |
| `--danger-soft` | `#fdeceb` | `#2c1614` |
| `--info` | `#1d5fb0` | `#6aa9f0` |
| `--info-soft` | `#e7f0fb` | `#101f30` |

### Shadows

| Token | Light | Dark |
|-------|-------|------|
| `--shadow-sm` | `0 6px 16px rgb(17 17 17 / 4%)` | `0 6px 16px rgb(0 0 0 / 45%)` |
| `--shadow-md` | `0 12px 28px rgb(17 17 17 / 6%)` | `0 12px 30px rgb(0 0 0 / 55%)` |
| `--shadow-lg` | `0 20px 52px rgb(17 17 17 / 8%)` | `0 22px 54px rgb(0 0 0 / 60%)` |

### Backward-compatible aliases

| Alias | Maps to |
|-------|---------|
| `--text` | `--foreground` |
| `--surface-soft` | `--surface-muted` |
| `--primary-bright` | `--accent` |
| `--primary-deep` | `--primary-hover` |
| `--shadow` | `--shadow-lg` |

### Product-site aliases (fixna-web only, in `design-system.css`)

| Alias | Maps to |
|-------|---------|
| `--ink` | `--primary` |
| `--ink-strong` | `--primary-hover` |
| `--ink-soft` | `--muted-foreground` |
| `--lime` | `--accent` |
| `--lime-soft` | `--accent` |
| `--surface` | `--surface` |
| `--surface-soft` | `--surface-muted` |
| `--line` | `--border` |
| `--white` | `--primary-foreground` |
| `--surface-hero` | `#000000` (light) / `#0f1110` (dark) | Theme-aware hero/CTA background |

---

## 3. Typography

### Font stack (`--font-sans`)

```
Inter, "Segoe UI", system-ui, -apple-system, sans-serif
```

Monospace (`--font-mono`):
```
ui-monospace, "SFMono-Regular", "Cascadia Code", Menlo, monospace
```

### Font sizes

| Token | Value |
|-------|-------|
| `--text-xs` | 0.72rem |
| `--text-sm` | 0.82rem |
| `--text-base` | 0.95rem |
| `--text-lg` | 1.08rem |
| `--text-xl` | 1.35rem |
| `--text-2xl` | clamp(1.6rem, 3vw, 2rem) |
| `--text-3xl` | clamp(2rem, 5vw, 3.1rem) |

### Line heights

| Token | Value |
|-------|-------|
| `--leading-normal` | 1.6 |
| `--leading-tight` | 1.15 |

### Heading styling

- `h1`: clamp(2.5rem, 5vw, 4.5rem), line-height 1.08, letter-spacing -0.02em, color var(--ink)
- `h2`: clamp(2rem, 4vw, 3.1rem), line-height 1.08, letter-spacing -0.02em, color var(--ink)
- `h3`: 1.18rem, line-height 1.08, color var(--ink)
- On dark hero sections: headings use `var(--primary-foreground)` (white) instead of var(--ink)
- `strong, h1, h2, h3`: letter-spacing -0.02em

### Body text

- `p`: color `var(--muted)`, line-height 1.6
- Links: color `var(--link)`, underline-offset 0.2em, 2px underline on hover

### Product-specific font overrides

- **fixna-web** (`design-system.css`): uses `var(--font-sans)`, `--text-base` size, `--leading-normal` line-height
- **consulting-web** (`globals.css`): uses `var(--font-sans)`, `--text-base` size, `--leading-normal` line-height, heading letter-spacing -0.02em
- **localboost-web** (`globals.css`): uses `var(--font-sans)`, `--text-base` size, `--leading-normal` line-height, heading letter-spacing -0.02em
- **hospitality-web, website-audit-web, invoice-assistant-web**: Tailwind apps — use Tailwind's default font stack which maps to `--font-sans` via `@theme`

---

## 4. Spacing & radii

| Token | Value |
|-------|-------|
| `--space-1` | 0.25rem |
| `--space-2` | 0.5rem |
| `--space-3` | 0.75rem |
| `--space-4` | 1rem |
| `--space-5` | 1.5rem |
| `--space-6` | 2rem |
| `--space-7` | 3rem |
| `--radius-sm` | 8px |
| `--radius-md` | 12px |
| `--radius-lg` | 16px |
| `--radius-xl` | 24px |
| `--radius-pill` | 999px |
| `--container` | 1180px |
| `--container-wide` | 1440px |
| `--transition` | 180ms ease |

---

## 5. Key components & patterns

### Focus ring (WCAG 2.2 compliant)

```css
:focus-visible {
  outline: 2px solid var(--ring);
  outline-offset: 2px;
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent) 55%, transparent);
}
```

- `--ring` is blue (#2563eb) in light theme, light blue (#60a5fa) in dark
- Outer glow uses lime accent at 55% opacity
- fixna-web design-system.css overrides with `--ink` outline + `--lime` glow (3px offset)
- consulting-web/localboost-web: use `color-mix(in srgb, var(--ring) 22%, transparent)` for form inputs

### Buttons

| Class | Style |
|-------|-------|
| `.button` / `.button-primary` | Background `--primary`, text `--primary-foreground` |
| `.button-secondary` | Background `--surface`, border `--border`, text `--foreground` |
| `.button-quiet` | Transparent background, text `--muted-foreground` |
| `.button-ghost` (fixna-web) | White border/opacity, text `--primary-foreground` |
| `.button-destructive` | Background `--danger`, text white |

Hover: primary→`--primary-hover`, secondary→`--surface-muted`, with `transform: translateY(-1px)`

### Dark backgrounds (hero, page-hero, CTA band)

- **Token:** `--surface-hero` = `#000000` (pure black)
- **Text color:** `var(--primary-foreground)` = `#ffffff` in all themes
- **Accent:** lime `rgb(199 237 148 / N%)` for borders, glows, decorative elements
- Used by: `.hero`, `.page-hero`, `.cta-band__inner` in fixna-web; `.home-intro` in consulting-web; `.auth-story` in localboost-web

### Cards

- Background: `var(--surface)`
- Border: `var(--border)`
- Shadow: `var(--shadow-sm)`
- Padding: clamp(1.4rem, 2.5vw, 2rem)

### Form inputs

- Border: `var(--border)`
- Background: `var(--input)`
- Focus: border `var(--ring)`, shadow `0 0 0 3px color-mix(in srgb, var(--ring) 22%, transparent)`

---

## 6. Recent style changes (2026-10-13)

### Color palette — neutralized (matching aiwithhassan.com)

**Before (green-tinted, unprofessional):**
- Light bg: `#f4f7f3` (soft green-tinted)
- Dark bg: `#0b1a14` (deep green-tinted)
- Light text: `#17352e` (dark green)
- Dark text: `#e7efe9` (green-tinted)
- Muted text: `#5c6f66` (green-tinted)
- Focus ring: `#163e32` (green)

**After (neutral, professional):**
- Light bg: `#ffffff` (pure white)
- Dark bg: `#000000` (pure black)
- Light text: `#111827` (near-black)
- Dark text: `#e5e7eb` (clean light gray)
- Muted text: `#6b7280` (neutral gray) / `#9ca3af` (dark)
- Focus ring: `#2563eb` (blue) / `#60a5fa` (dark)

### Font stack refined

- Removed "Aptos" from all font stacks
- Now: `Inter, "Segoe UI", system-ui, -apple-system, sans-serif`
- All products now reference `var(--font-sans)` token instead of hardcoded font strings
- Line height: `1.6` (was `1.65`)
- Heading letter-spacing: `-0.02em` (was `-0.03em` to `-0.05em` across products)

### Bug fixes

1. **Orphaned CSS in tokens.css:** The dark theme section was missing `--secondary`, `--muted`, `--border`, `--ring`, `--link`, status colors, and shadows. Lines 166-190 were orphaned outside any selector block (broken CSS). Fixed by properly defining all values inside `:root[data-theme="dark"]` and the system-preference media query.

2. **Text color on dark backgrounds:** Dark hero/CTA sections used `color: var(--surface)` which resolves to `#111111` in dark theme — invisible on black. Fixed by using `color: var(--primary-foreground)` (#ffffff) in:
   - `.hero` body text
   - `.hero h1/h2` / `.page-hero h1/h2` headings
   - `.cta-band__inner` body text
   - `.cta-band h2` heading
   - `.page-hero` body text
   - `.hero-stat-card strong` values
   - `.button-ghost` text

### Files changed

| File | Changes |
|------|---------|
| `packages/fixna-design-system/src/tokens.css` | Complete neutral palette, fixed dark theme structure, font stack, line height |
| `frontend/fixna-web/src/app/design-system.css` | `--surface-hero` token, hero/CTA text colors, font/line-height tokens, focus ring |
| `frontend/consulting-web/src/app/globals.css` | Font stack token, home-intro gradient to pure black, green-tinted colors removed |
| `frontend/localboost-web/src/app/globals.css` | Font stack token, letter-spacing, opacity reduced on radial gradient |
| `frontend/localboost-web/src/app/product.css` | Auth story gradient to neutral, green-tinted borders/shadows/text removed |
| `frontend/localboost-web/src/app/responsive.css` | Green-tinted shadow on resource-list items |

---

## 7. Testing

| Project | Command | Tests | Status |
|---------|---------|-------|--------|
| fixna-web | `npx vitest run` | 25 | All passing |
| fixna-design-system | `npx vitest run` | 18 (theme.test.ts) | All passing |
| fixna-web | `npx tsc --noEmit` | — | No errors |
| localboost-web | `npx tsc --noEmit` | — | No errors |
| consulting-web | `npx tsc --noEmit` | — | No errors |

---

## 8. Do NOT share externally

Neon credentials, JWT secrets, database passwords, API keys, demo user plaintext
password, or Render/Vercel account tokens.
