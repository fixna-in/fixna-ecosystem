import type { Metadata } from "next";

import { resolveSiteUrl } from "./site-url";

/**
 * Absolute canonical URL for a public content path (R1).
 * Resolves the site URL lazily (not via the module-level `siteUrl`
 * singleton) so the build-time NEXT_PUBLIC_SITE_URL is always honoured and
 * the helper stays unit-testable under `vi.stubEnv`.
 * The legacy `/capabilities` route keeps its own canonical override and is
 * intentionally excluded from the sitemap — do not reuse this helper there.
 */
export function canonicalFor(path: string): Metadata {
  const base = resolveSiteUrl(process.env.NEXT_PUBLIC_SITE_URL);
  return {
    alternates: { canonical: `${base}${path}` },
    openGraph: { url: path === "" ? "/" : path },
  };
}
