"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  applyTheme,
  coercePreference,
  readStoredPreference,
  resolveTheme,
  systemTheme,
  writeStoredPreference,
  type ResolvedTheme,
  type ThemePreference,
} from "./theme";

interface ThemeContextValue {
  /** The user's chosen preference (light | dark | system). */
  preference: ThemePreference;
  /** The concrete theme currently applied (light | dark). */
  resolved: ResolvedTheme;
  /** Set an explicit preference and persist it. */
  setPreference: (preference: ThemePreference) => void;
  /** Convenience: toggle strictly between light and dark. */
  toggle: () => void;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export interface ThemeProviderProps {
  children: ReactNode;
  /** Override the initial preference (mostly for tests/SSR). */
  defaultPreference?: ThemePreference;
}

/**
 * Provides theme state to the tree. Reads the persisted preference on mount,
 * applies the resolved theme to <html>, and follows OS changes while the
 * preference is "system". Changing the theme touches only presentation — it
 * never triggers API requests or resets application/query state.
 */
export function ThemeProvider({
  children,
  defaultPreference,
}: ThemeProviderProps) {
  const [preference, setPreferenceState] = useState<ThemePreference>(() =>
    defaultPreference ?? readStoredPreference(),
  );
  const [resolved, setResolved] = useState<ResolvedTheme>(() =>
    resolveTheme(defaultPreference ?? readStoredPreference()),
  );

  // Apply the resolved theme whenever it changes.
  useEffect(() => {
    applyTheme(resolved);
  }, [resolved]);

  // Follow OS preference changes only while preference === "system".
  useEffect(() => {
    if (preference !== "system") return;
    if (typeof window === "undefined" || !window.matchMedia) return;
    const mql = window.matchMedia("(prefers-color-scheme: dark)");
    const onChange = () => setResolved(systemTheme());
    onChange();
    mql.addEventListener("change", onChange);
    return () => mql.removeEventListener("change", onChange);
  }, [preference]);

  const setPreference = useCallback((next: ThemePreference) => {
    const pref = coercePreference(next);
    setPreferenceState(pref);
    setResolved(resolveTheme(pref));
    writeStoredPreference(pref);
  }, []);

  const toggle = useCallback(() => {
    setPreference(resolved === "dark" ? "light" : "dark");
  }, [resolved, setPreference]);

  const value = useMemo<ThemeContextValue>(
    () => ({ preference, resolved, setPreference, toggle }),
    [preference, resolved, setPreference, toggle],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

/** Access the current theme state. Must be used within a ThemeProvider. */
export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (ctx === undefined) {
    throw new Error("useTheme must be used within a ThemeProvider");
  }
  return ctx;
}
