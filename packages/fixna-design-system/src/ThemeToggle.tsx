"use client";

import { useTheme } from "./ThemeProvider";
import type { ResolvedTheme } from "./theme";

export interface ThemeToggleProps {
  /** Accessible label prefix, e.g. "Toggle theme" → "Toggle theme (currently dark)". */
  label?: string;
  className?: string;
}

function SunIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="4.2" stroke="currentColor" strokeWidth="1.8" />
      <path
        d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5 5l1.4 1.4M17.6 17.6L19 19M19 5l-1.4 1.4M6.4 17.6L5 19"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
    </svg>
  );
}

function MoonIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M20 14.5A8.5 8.5 0 0 1 9.5 4a7 7 0 1 0 10.5 10.5Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
    </svg>
  );
}

/**
 * Accessible light/dark toggle. Renders an icon button with a dynamic
 * accessible name and tooltip so its purpose is never conveyed by icon alone.
 * Toggles strictly between light and dark for immediate, predictable control.
 */
export function ThemeToggle({ label = "Toggle theme", className }: ThemeToggleProps) {
  const { resolved, toggle } = useTheme();
  const next: ResolvedTheme = resolved === "dark" ? "light" : "dark";
  const accessibleName = `${label} — switch to ${next} mode (currently ${resolved})`;

  return (
    <button
      type="button"
      className={className ?? "theme-toggle"}
      onClick={toggle}
      aria-label={accessibleName}
      title={`Switch to ${next} mode`}
      data-theme-state={resolved}
    >
      {resolved === "dark" ? <SunIcon /> : <MoonIcon />}
    </button>
  );
}
