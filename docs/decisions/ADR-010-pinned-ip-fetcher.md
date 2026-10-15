# ADR-010: Pinned-IP Fetcher for SSRF and DNS-Rebinding Defense

## Status

Proposed (2026-10-12) — pending approval ([decision register](../specs/README.md))

## Context

`WebsiteFetcher` (MVP) resolves DNS and validates every address against private/loopback/link-local/CGNAT/ULA ranges before connecting — but the HTTP client re-resolves at connect time. A DNS rebinding answer (public IP at check time, private at connect) can bypass the guard. This is a documented MVP limitation.

## Decision

1. **Resolve once, connect to a pinned IP:**
   - Resolve the host (A + AAAA) via the platform resolver and validate **every** returned address; reject if any is disallowed (fail-closed).
   - Connect directly to the validated IP; send the original hostname in the HTTP `Host` header and TLS SNI, and verify the certificate against the original hostname.
2. **No redirects** (unchanged) — every fetched URL (page, robots.txt, sitemap.xml) runs the full resolve → validate → pin pipeline; internal hops = 0.
3. **Freshness:** DNS cache TTL ≤ 30s; each crawl attempt re-resolves — no long-lived cache.
4. **Scheme/port allowlist unchanged:** http/https only; ports 80/443 only.
5. **Denylist additions:** cloud metadata endpoints (`169.254.169.254`, `metadata.google.internal`), decimal/octal/hex IP-literal forms normalized via `InetAddress` before range checks, and literal-IP URLs with non-standard ports rejected.
6. Policy violations surface as the `http_status` check with evidence `{blocked: "ssrf_policy"}` — never an exception leaking internal addresses.
7. **Testability:** a resolver seam in `WebsiteFetcher`; unit tests simulate rebinding (resolver returns public then private answers) and assert the connect is refused at pin time.

## Consequences

- Closes the documented rebinding gap with no new dependencies (JDK resolver + client-level pinning).
- Deterministic SSRF tests become possible (currently limited to static range checks).
- Multi-IP CDN hosts still work: all IPs are validated, the first valid one is pinned.
- HTTP/2 connection reuse and transparent proxy support are explicitly out of scope for this design.
