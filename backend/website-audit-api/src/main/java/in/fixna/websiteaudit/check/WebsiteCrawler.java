package in.fixna.websiteaudit.check;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import in.fixna.websiteaudit.fetcher.WebsiteFetcher;
import in.fixna.websiteaudit.fetcher.WebsiteFetcher.FetchedPage;

/**
 * Bounded same-site crawler (ADR-009). Hard limits are server-enforced and
 * clamped regardless of caller input: max 25 stored pages, depth ≤ 2
 * (seed = depth 0), max 40 fetch attempts, 60s wall-clock budget,
 * 500ms per-host politeness delay, robots.txt honored, same host as the
 * seed only. Pages are returned seed-first.
 */
@Component
public class WebsiteCrawler {

    private static final Pattern HREF = Pattern.compile("(?i)href\\s*=\\s*[\"']([^\"'#]+)[\"']");

    /** Hard server-side bounds; never accept larger values from callers. */
    public record CrawlBounds(
            int maxPages, int maxDepth, int maxFetchAttempts, long wallClockMs, long perHostDelayMs) {

        public static CrawlBounds defaults() {
            return new CrawlBounds(25, 2, 40, 60_000, 500);
        }

        public static CrawlBounds fast() {
            return new CrawlBounds(25, 2, 40, 60_000, 0);
        }
    }

    /** Outcome of one bounded crawl. {@code pages} is seed-first. */
    public record CrawlResult(
            FetchedPage seed,
            FetchedPage robots,
            FetchedPage sitemap,
            List<FetchedPage> pages,
            int fetchAttempts,
            int linksDiscovered,
            boolean timeBudgetExceeded) {}

    private final WebsiteFetcher fetcher;

    public WebsiteCrawler(WebsiteFetcher fetcher) {
        this.fetcher = fetcher;
    }

    public CrawlResult crawl(URI seedUri, boolean deep, CrawlBounds bounds) {
        Instant deadline = Instant.now().plusMillis(bounds.wallClockMs());
        int attempts = 0;

        attempts++;
        FetchedPage seed = fetcher.fetch(seedUri).orElse(null);
        attempts++;
        FetchedPage robots = fetcher.fetch(WebsiteFetcher.sibling(seedUri, "/robots.txt")).orElse(null);
        attempts++;
        FetchedPage sitemap = fetcher.fetch(WebsiteFetcher.sibling(seedUri, "/sitemap.xml")).orElse(null);

        List<FetchedPage> pages = new ArrayList<>();
        if (seed == null) {
            return new CrawlResult(null, robots, sitemap, pages, attempts, 0, false);
        }
        pages.add(seed);
        if (!deep || !isHtml(seed) || seed.statusCode() < 200 || seed.statusCode() >= 300) {
            return new CrawlResult(seed, robots, sitemap, pages, attempts, 0, false);
        }

        Set<String> disallowed = robotsDisallowedPrefixes(robots);
        Set<String> seen = new HashSet<>();
        seen.add(normalize(seedUri));
        Deque<URI> queue = new ArrayDeque<>();
        Deque<Integer> depths = new ArrayDeque<>();
        int linksFound = 0;
        for (URI link : extractLinks(seed.body(), seedUri)) {
            linksFound++;
            String normalized = normalize(link);
            if (normalized != null && seen.add(normalized) && !isDisallowed(normalized, disallowed)) {
                queue.add(link);
                depths.add(1);
            }
        }

        boolean timedOut = false;
        while (!queue.isEmpty() && pages.size() < bounds.maxPages()) {
            if (attempts >= bounds.maxFetchAttempts() || Instant.now().isAfter(deadline)) {
                timedOut = true;
                break;
            }
            URI next = queue.poll();
            int depth = depths.poll();
            if (bounds.perHostDelayMs() > 0) {
                sleep(bounds.perHostDelayMs());
            }
            attempts++;
            FetchedPage page = fetcher.fetch(next).orElse(null);
            if (page == null) {
                continue;
            }
            pages.add(page);
            if (depth < bounds.maxDepth() && isHtml(page)
                    && page.statusCode() >= 200 && page.statusCode() < 300) {
                for (URI link : extractLinks(page.body(), next)) {
                    linksFound++;
                    String normalized = normalize(link);
                    if (normalized != null && seen.add(normalized) && !isDisallowed(normalized, disallowed)) {
                        queue.add(link);
                        depths.add(depth + 1);
                    }
                }
            }
        }
        return new CrawlResult(seed, robots, sitemap, pages, attempts, linksFound, timedOut);
    }

    /** Extracts and resolves same-host http(s) links from an HTML body. */
    static List<URI> extractLinks(String html, URI base) {
        List<URI> links = new ArrayList<>();
        if (html == null || html.isBlank()) {
            return links;
        }
        Matcher matcher = HREF.matcher(html);
        while (matcher.find()) {
            String raw = matcher.group(1).trim();
            try {
                URI resolved = base.resolve(raw);
                String scheme = resolved.getScheme();
                boolean http = scheme != null
                        && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
                boolean sameHost = resolved.getHost() != null
                        && resolved.getHost().equalsIgnoreCase(base.getHost());
                if (http && sameHost) {
                    links.add(resolved);
                }
            } catch (IllegalArgumentException ignored) {
                // malformed link — skip
            }
        }
        return links;
    }

    /** Parses Disallow rules from robots.txt (all user-agent groups). */
    static Set<String> robotsDisallowedPrefixes(FetchedPage robots) {
        Set<String> prefixes = new HashSet<>();
        if (robots == null || robots.statusCode() != 200 || robots.body() == null) {
            return prefixes;
        }
        for (String line : robots.body().split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("disallow:")) {
                String path = trimmed.substring("disallow:".length()).trim();
                if (!path.isBlank()) {
                    prefixes.add(path);
                }
            }
        }
        return prefixes;
    }

    private static boolean isDisallowed(String normalizedUrl, Set<String> prefixes) {
        if (prefixes.isEmpty()) {
            return false;
        }
        try {
            String path = URI.create(normalizedUrl).getRawPath();
            path = path == null || path.isBlank() ? "/" : path;
            for (String prefix : prefixes) {
                if (path.startsWith(prefix)) {
                    return true;
                }
            }
        } catch (IllegalArgumentException ignored) {
            return false;
        }
        return false;
    }

    private static String normalize(URI uri) {
        if (uri.getHost() == null) {
            return null;
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
        return scheme + "://" + host + path;
    }

    private static boolean isHtml(FetchedPage page) {
        return page.contentType() == null || page.contentType().toLowerCase(Locale.ROOT).contains("html");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}