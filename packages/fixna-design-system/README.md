# Fixna Design System (`@fixna/design-system`)

The single source of truth for the Fixna ecosystem's visual language: **semantic
design tokens**, **light/dark themes**, a **theme runtime**, and shared
**primitives**. Every Fixna web application consumes this package — do not
invent per-app color palettes, button styles, or card systems.

Brand: primary dark green `#163e32` · accent lime `#c7ed94`.

## What's inside

| Path | Purpose |
|------|---------|
| `src/tokens.css` | Semantic tokens for **light** (`:root`) and **dark** (`:root[data-theme="dark"]`), plus a `prefers-color-scheme` fallback and backward-compatible aliases (`--text`, `--surface-soft`, `--primary-bright`, `--primary-deep`). |
| `src/base.css` | Optional token-driven primitives for plain-CSS apps: buttons, cards, inputs, tables, badges, alerts, focus ring, skip-link, **theme toggle**, skeletons, reduced-motion. |
| `src/tailwind-theme.css` | Tailwind v4 bridge: maps tokens to utilities (`bg-surface`, `text-foreground`, `bg-primary`, …) and redirects the `dark:` variant to the `[data-theme="dark"]` toggle. Also ships the shared `.theme-toggle` control so Tailwind apps don't re-declare it. |
| `src/theme.ts` | Framework-neutral theme logic (resolve/persist/apply + no-flash `themeInitScript`). |
| `src/ThemeProvider.tsx` | `ThemeProvider` + `useTheme` — `light | dark | system`, persisted per-origin, follows OS while on "system". |
| `src/ThemeToggle.tsx` | Accessible icon toggle with dynamic accessible name + tooltip. |

## Semantic tokens

`background` · `foreground` · `surface` · `surface-elevated` · `surface-muted` ·
`primary` · `primary-foreground` · `accent` · `accent-foreground` · `secondary` ·
`muted` · `muted-foreground` · `border` · `input` · `ring` · `link` · `success` ·
`warning` · `danger` · `info` · `sidebar-background` · `sidebar-foreground` ·
`header-background` · `overlay-background` (+ `*-soft` status backgrounds).

Changing a token updates **every consuming app and both themes** at once.

## Usage — plain-CSS apps (LocalBoost, Consulting, corporate site)

1. In `layout.tsx`:
   ```tsx
   import { ThemeProvider } from "@fixna/design-system";
   import { themeInitScript } from "@fixna/design-system/theme";
   import "@fixna/design-system/tokens.css";
   import "@fixna/design-system/base.css";
   import "./globals.css"; // existing app CSS now inherits tokens + dark
   ```
   Add `<html lang="en" suppressHydrationWarning>`, inject the no-flash script
   in `<head>` (`<script dangerouslySetInnerHTML={{ __html: themeInitScript }} />`),
   and wrap the tree in `<ThemeProvider>`.
2. Replace hardcoded `:root` color blocks in `globals.css` with token references
   (or delete them so `tokens.css` is authoritative). The backward-compat
   aliases keep existing selectors working during migration.
3. Render `<ThemeToggle />` in the header/sidebar.

## Usage — Tailwind apps (Invoice Assistant, Website Audit, Hospitality)

In `globals.css`, load the bridge **after** Tailwind:
```css
@import "@fixna/design-system/tokens.css";
@import "@fixna/design-system/tailwind-theme.css";
@import "tailwindcss";
```
Then wrap `layout.tsx` in `<ThemeProvider>` (with the no-flash script) and use
token utilities (`bg-surface text-foreground border-border rounded-lg`) plus the
existing `dark:` variant — which now follows the toggle instead of only the OS.

## Theme behavior

- `light` / `dark` / `system`; **explicit choice overrides** the OS preference.
- Persists to `localStorage` key `fixna.theme` (origin-scoped — apps are on
  separate origins, so preferences are intentionally per-app). No backend, no
  auth coupling, purely a visual preference.
- Applies immediately, survives refresh and route navigation, avoids a flash of
  the wrong theme via the pre-paint script, and never triggers API calls or
  resets app/query state.

## Deployment note

This package is consumed at **build time** (CSS + TS compiled into each app's
bundle). There is no separate runtime service to release. Because it is a
workspace dependency, run a normal `npm install` at the repo root after pulling;
each app still builds and deploys independently.
