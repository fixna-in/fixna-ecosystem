import assert from "node:assert/strict";
import test from "node:test";

import { resolveSiteUrl } from "./site-url.ts";

test("normalizes a bare production hostname to https", () => {
  assert.equal(resolveSiteUrl("fixna.in"), "https://fixna.in");
});

test("preserves an explicit https production URL", () => {
  assert.equal(resolveSiteUrl("https://fixna.in"), "https://fixna.in");
});

test("uses the local fallback when the value is invalid", () => {
  assert.equal(resolveSiteUrl("not a valid url"), "http://localhost:3002");
});
