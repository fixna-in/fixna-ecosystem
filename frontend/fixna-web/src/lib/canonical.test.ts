import { describe, expect, it, vi } from "vitest";

import { canonicalFor } from "./canonical";
import { resolveSiteUrl } from "./site-url";

/**
 * Canonical URL contract (R1).
 * `siteUrl` is env-dependent (falls back to localhost without
 * NEXT_PUBLIC_SITE_URL), so the www-host contract is asserted on the pure
 * `resolveSiteUrl` — the same function `siteUrl` is built from — while
 * `canonicalFor` wiring is asserted structurally.
 */
describe("canonical URL contract", () => {
  it("resolves the production env value to the www canonical host", () => {
    expect(resolveSiteUrl("https://www.fixna.in")).toBe("https://www.fixna.in");
    expect(resolveSiteUrl(undefined)).toBe("http://localhost:3002");
  });

  it("builds absolute canonical URLs for content paths", () => {
    vi.stubEnv("NEXT_PUBLIC_SITE_URL", "https://www.fixna.in");
    for (const path of ["", "/about", "/what-we-do", "/products"]) {
      const metadata = canonicalFor(path);
      const canonical = metadata.alternates?.canonical as string;
      expect(canonical).toBe(`https://www.fixna.in${path}`);
    }
    vi.unstubAllEnvs();
  });

  it("keeps the legacy capabilities canonical pointed at /what-we-do", () => {
    expect(`${resolveSiteUrl("https://www.fixna.in")}/what-we-do`).toBe(
      "https://www.fixna.in/what-we-do",
    );
  });
});
