import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import {
  THEME_STORAGE_KEY,
  coercePreference,
  resolveTheme,
  readStoredPreference,
  writeStoredPreference,
  systemTheme,
  applyTheme,
} from "./theme";

/** Minimal in-memory Storage stub. */
function makeStorage(initial: Record<string, string> = {}) {
  const map = new Map<string, string>(Object.entries(initial));
  return {
    getItem: (k: string) => (map.has(k) ? map.get(k)! : null),
    setItem: (k: string, v: string) => void map.set(k, v),
    removeItem: (k: string) => void map.delete(k),
    _map: map,
  };
}

describe("coercePreference", () => {
  it.each(["light", "dark", "system"] as const)("keeps valid preference %s", (p) => {
    expect(coercePreference(p)).toBe(p);
  });

  it.each([undefined, null, "", "LIGHT", "auto", 42, {}])(
    "falls back to system for invalid value %s",
    (v) => expect(coercePreference(v as unknown)).toBe("system"),
  );
});

describe("systemTheme", () => {
  const original = globalThis.window;
  afterEach(() => {
    globalThis.window = original as unknown as typeof globalThis.window;
  });

  function setMatch(dark: boolean) {
    globalThis.window = {
      matchMedia: (q: string) =>
        ({ matches: q.includes("dark") ? dark : false }) as MediaQueryList,
    } as unknown as Window & typeof globalThis;
  }

  it("returns dark when the OS prefers dark", () => {
    setMatch(true);
    expect(systemTheme()).toBe("dark");
  });
  it("returns light when the OS prefers light", () => {
    setMatch(false);
    expect(systemTheme()).toBe("light");
  });
});

describe("resolveTheme", () => {
  beforeEach(() => {
    globalThis.window = {
      matchMedia: () => ({ matches: true }) as MediaQueryList,
    } as unknown as Window & typeof globalThis;
  });
  afterEach(() => {
    globalThis.window = undefined as unknown as typeof globalThis.window;
  });

  it("passes explicit preferences straight through", () => {
    expect(resolveTheme("light")).toBe("light");
    expect(resolveTheme("dark")).toBe("dark");
  });
  it("resolves system to the OS preference", () => {
    expect(resolveTheme("system")).toBe("dark");
  });
});

describe("preference persistence", () => {
  it("reads the stored preference, defaulting to system", () => {
    expect(readStoredPreference(makeStorage() as never)).toBe("system");
    expect(readStoredPreference(makeStorage({ [THEME_STORAGE_KEY]: "dark" }) as never)).toBe("dark");
  });

  it("writes the preference under the shared key", () => {
    const s = makeStorage();
    writeStoredPreference("light", s as never);
    expect(s._map.get(THEME_STORAGE_KEY)).toBe("light");
  });

  it("never throws when storage is unavailable", () => {
    const throwing = {
      getItem: () => {
        throw new Error("blocked");
      },
      setItem: () => {
        throw new Error("blocked");
      },
    };
    expect(() => readStoredPreference(throwing as never)).not.toThrow();
    expect(() => writeStoredPreference("dark", throwing as never)).not.toThrow();
    expect(readStoredPreference(throwing as never)).toBe("system");
  });
});

describe("applyTheme", () => {
  it("sets the data-theme attribute and color-scheme on <html>", () => {
    const attrs: Record<string, string> = {};
    const doc = {
      documentElement: {
        setAttribute: (k: string, v: string) => (attrs[k] = v),
        style: {} as { colorScheme: string },
      },
    } as unknown as Document;
    applyTheme("dark", doc);
    expect(attrs["data-theme"]).toBe("dark");
    expect((doc.documentElement.style as { colorScheme: string }).colorScheme).toBe("dark");
  });
});
