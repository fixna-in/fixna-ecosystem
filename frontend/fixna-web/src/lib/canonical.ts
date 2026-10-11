import type { Metadata } from "next";

import { siteUrl } from "@/lib/site-url";

/**
 * Absolute canonical URL for a public content path (R1).
 * The legacy `/capabilities` route keeps its own canonical override and is
 * intentionally excluded from the sitemap — do not reuse this helper there.
 */
export function canonicalFor(path: string): Metadata {
  return {
    alternates: { canonical: `${siteUrl}${path}` },
    openGraph: { url: path === "" ? "/" : path },
  };
}
