/**
 * Fixna Unified Design System — public entry point.
 *
 * Single source of truth for Fixna brand tokens, light/dark themes, and the
 * theme runtime shared across every Fixna web application.
 *
 * CSS is imported directly from the package, e.g.:
 *   import "@fixna/design-system/tokens.css";
 *   import "@fixna/design-system/base.css";            // plain-CSS apps
 *   import "@fixna/design-system/tailwind-theme.css";  // Tailwind apps
 */

// Re-export the canonical brand mark so apps have one import surface.
export { BrandMark, BRAND_MARK_SVG } from "@fixna/brand";
export type { BrandMarkProps } from "@fixna/brand";

// Theme runtime.
export { ThemeProvider, useTheme } from "./ThemeProvider";
export type { ThemeProviderProps } from "./ThemeProvider";
export { ThemeToggle } from "./ThemeToggle";
export type { ThemeToggleProps } from "./ThemeToggle";

// Theme logic (pure helpers, also unit-tested).
export {
  THEME_STORAGE_KEY,
  THEME_ATTRIBUTE,
  THEME_PREFERENCES,
  themeInitScript,
  coercePreference,
  systemTheme,
  resolveTheme,
  readStoredPreference,
  writeStoredPreference,
  applyTheme,
} from "./theme";
export type { ThemePreference, ResolvedTheme } from "./theme";
