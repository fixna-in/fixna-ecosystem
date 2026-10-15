/**
 * Fixna Design System — theme preference logic (framework-neutral).
 *
 * A theme "preference" is what the user chose: "light", "dark", or "system".
 * The "resolved" theme is the concrete mode actually applied to the DOM:
 * "system" resolves to the OS preference (light | dark).
 *
 * Persistence: browser localStorage, origin-scoped. Each Fixna app is deployed
 * on its own origin, so preferences are intentionally stored separately per app
 * via a shared key. This is a non-sensitive visual preference — no backend
 * schema or auth coupling is introduced.
 */

export type ThemePreference = "light" | "dark" | "system";
export type ResolvedTheme = "light" | "dark";

export const THEME_STORAGE_KEY = "fixna.theme";
export const THEME_ATTRIBUTE = "data-theme";

export const THEME_PREFERENCES: readonly ThemePreference[] = [
  "light",
  "dark",
  "system",
];

/** Narrow an unknown stored value to a valid preference, defaulting to system. */
export function coercePreference(value: unknown): ThemePreference {
  return value === "light" || value === "dark" || value === "system"
    ? value
    : "system";
}

/** Read the OS color-scheme preference. Falls back to light when unknown. */
export function systemTheme(): ResolvedTheme {
  if (typeof window === "undefined" || !window.matchMedia) return "light";
  return window.matchMedia("(prefers-color-scheme: dark)").matches
    ? "dark"
    : "light";
}

/** Resolve a preference into the concrete theme to apply. */
export function resolveTheme(preference: ThemePreference): ResolvedTheme {
  return preference === "system" ? systemTheme() : preference;
}

/** Read the persisted preference from storage; returns "system" if unset. */
export function readStoredPreference(
  storage: Pick<Storage, "getItem"> | undefined = safeLocalStorage(),
): ThemePreference {
  if (!storage) return "system";
  try {
    return coercePreference(storage.getItem(THEME_STORAGE_KEY));
  } catch {
    return "system";
  }
}

/** Persist the preference. Wrapped so private-mode/security errors never throw. */
export function writeStoredPreference(
  preference: ThemePreference,
  storage: Pick<Storage, "setItem"> | undefined = safeLocalStorage(),
): void {
  if (!storage) return;
  try {
    storage.setItem(THEME_STORAGE_KEY, preference);
  } catch {
    /* storage unavailable (private mode / disabled) — preference just won't persist */
  }
}

/** Apply a resolved theme to the document element (color-scheme + attribute). */
export function applyTheme(resolved: ResolvedTheme, doc?: Document): void {
  const target = doc ?? (typeof document !== "undefined" ? document : undefined);
  if (!target?.documentElement) return;
  target.documentElement.setAttribute(THEME_ATTRIBUTE, resolved);
  target.documentElement.style.colorScheme = resolved;
}

function safeLocalStorage(): Storage | undefined {
  try {
    if (typeof window === "undefined") return undefined;
    return window.localStorage;
  } catch {
    return undefined;
  }
}

/**
 * Inline bootstrap run before first paint to avoid a flash of the wrong theme.
 * Reads the stored preference (falling back to the OS preference) and sets the
 * data-theme attribute + color-scheme on <html> synchronously.
 *
 * Inject via: <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
 */
const INLINE_SCRIPT_ESCAPE_MAP: Record<string, string> = {
  "<": "\\u003C",
  ">": "\\u003E",
  "/": "\\u002F",
  "\\": "\\\\",
  "\b": "\\b",
  "\f": "\\f",
  "\n": "\\n",
  "\r": "\\r",
  "\t": "\\t",
  "\0": "\\0",
  "\u2028": "\\u2028",
  "\u2029": "\\u2029",
};

function escapeForInlineScript(value: string): string {
  return value.replace(/[<>\/\\\b\f\n\r\t\0\u2028\u2029]/g, (ch) => INLINE_SCRIPT_ESCAPE_MAP[ch] ?? ch);
}

export const themeInitScript = `(function(){try{var p=localStorage.getItem(${escapeForInlineScript(
  JSON.stringify(THEME_STORAGE_KEY),
)});var d=window.matchMedia&&window.matchMedia('(prefers-color-scheme: dark)').matches;var r=p==='light'||p==='dark'?p:(d?'dark':'light');var e=document.documentElement;e.setAttribute(${escapeForInlineScript(
  JSON.stringify(THEME_ATTRIBUTE),
)},r);e.style.colorScheme=r;}catch(e){}})();`;
