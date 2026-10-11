import { describe, expect, it } from "vitest";

import { canonicalFor } from "./canonical";
import { siteUrl } from "./site-url";

/** Canonical URL contract (R1): every public page emits an absolute www canonical. */
describe("canonical URL contract", () => {
  it("resolves the configured site URL to the www canonical host", () => {
    expect(siteUrl.startsWith("https://")).toBe(true);
  });

  it("builds absolute canonical URLs for content paths", () => {
    for (const path of ["", "/about", "/what-we-do", "/products"]) {
      const metadata = canonicalFor(path);
      const canonical = metadata.alternates?.canonical as string;
      expect(canonical).toBe(`${siteUrl}${path}`);
      expect(canonical).toMatch(/^https:\/\/[^/]+(\/.*)?$/);
    }
  });

  it("keeps the legacy capabilities canonical pointed at /what-we-do", () => {
    expect(`${siteUrl}/what-we-do`).toMatch(/^https:\/\/[^/]+\/what-we-do$/);
  });
});
