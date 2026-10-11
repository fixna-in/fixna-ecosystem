import { describe, expect, it } from "vitest";
import { resolveSiteUrl } from "./site-url";

describe("resolveSiteUrl", () => {
  it("normalizes a bare production hostname to https", () => {
    expect(resolveSiteUrl("fixna.in")).toBe("https://fixna.in");
  });

  it("preserves an explicit https production URL", () => {
    expect(resolveSiteUrl("https://fixna.in")).toBe("https://fixna.in");
  });

  it("uses the local fallback when the value is invalid", () => {
    expect(resolveSiteUrl("not a valid url")).toBe("http://localhost:3002");
  });

  it("uses the local fallback when the value is empty", () => {
    expect(resolveSiteUrl(undefined)).toBe("http://localhost:3002");
    expect(resolveSiteUrl("   ")).toBe("http://localhost:3002");
  });
});
