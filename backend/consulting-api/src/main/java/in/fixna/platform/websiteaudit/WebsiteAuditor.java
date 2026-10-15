package in.fixna.platform.websiteaudit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import in.fixna.platform.websiteaudit.WebsiteFetcher.FetchedPage;

/**
 * Deterministic rule-based website auditor. No AI, no new dependencies —
 * lightweight regex inspection of fetched HTML. HTML checks are skipped
 * (not failed) when the response is not HTML or not a 2xx status.
 */
@Component
public class WebsiteAuditor {

    private static final Pattern TITLE = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final Pattern HTML_TAG = Pattern.compile("(?is)<html\\b[^>]*>");
    private static final Pattern META_TAG = Pattern.compile("(?is)<meta\\b[^>]*>");
    private static final Pattern LINK_TAG = Pattern.compile("(?is)<link\\b[^>]*>");
    private static final Pattern IMG_TAG = Pattern.compile("(?is)<img\\b[^>]*>");
    private static final Pattern H1 = Pattern.compile("(?is)<h1\\b[^>]*>");

    /** One deterministic check outcome. Score is 0, partial (1-99), or 100. */
    public record CheckResult(
            String code, AuditSeverity severity, int score, Map<String, String> evidence, String fixHint) {}

    /** All checks plus the overall score and a human-readable summary. */
    public record AuditOutcome(List<CheckResult> checks, int overallScore, String summary) {}

    public AuditOutcome audit(FetchedPage page, FetchedPage robots, FetchedPage sitemap) {
        List<CheckResult> checks = new ArrayList<>();

        int status = page.statusCode();
        boolean statusOk = status >= 200 && status < 300;
        checks.add(new CheckResult(
                "http_status",
                AuditSeverity.CRITICAL,
                statusOk ? 100 : 0,
                Map.of("statusCode", String.valueOf(status)),
                statusOk ? null : "Fix server errors; the page must return HTTP 200."));

        boolean https = "https".equalsIgnoreCase(page.finalUri().getScheme());
        checks.add(new CheckResult(
                "https",
                AuditSeverity.CRITICAL,
                https ? 100 : 0,
                Map.of("scheme", String.valueOf(page.finalUri().getScheme())),
                https ? null : "Serve the site over HTTPS and redirect HTTP to HTTPS."));

        if (isHtml(page) && statusOk) {
            checks.addAll(htmlChecks(page.body()));
        } else {
            checks.add(new CheckResult(
                    "html_content",
                    AuditSeverity.INFO,
                    100,
                    Map.of("contentType", page.contentType(), "reason", "HTML checks skipped"),
                    null));
        }

        long duration = page.durationMs();
        int timeScore = duration < 3_000 ? 100 : duration < 6_000 ? 50 : 0;
        checks.add(new CheckResult(
                "response_time",
                AuditSeverity.INFO,
                timeScore,
                Map.of("durationMs", String.valueOf(duration)),
                timeScore == 100 ? null : "Reduce server response time below 3 seconds."));

        boolean robotsOk = robots != null && robots.statusCode() == 200;
        checks.add(new CheckResult(
                "robots_txt",
                AuditSeverity.INFO,
                robotsOk ? 100 : 0,
                Map.of("statusCode", robots != null ? String.valueOf(robots.statusCode()) : "unreachable"),
                robotsOk ? null : "Publish /robots.txt and reference your sitemap in it."));

        boolean sitemapOk = sitemap != null && sitemap.statusCode() == 200;
        checks.add(new CheckResult(
                "sitemap_xml",
                AuditSeverity.INFO,
                sitemapOk ? 100 : 0,
                Map.of("statusCode", sitemap != null ? String.valueOf(sitemap.statusCode()) : "unreachable"),
                sitemapOk ? null : "Publish /sitemap.xml so search engines can discover pages."));

        int passed = (int) checks.stream().filter(c -> c.score() == 100).count();
        int overall = (int) Math.round(checks.stream().mapToInt(CheckResult::score).average().orElse(0));
        String summary = passed + " of " + checks.size() + " checks passed. Overall score " + overall + "/100.";
        return new AuditOutcome(List.copyOf(checks), overall, summary);
    }

    /** Builds an outcome for an unreachable target (audit marked FAILED). */
    public static AuditOutcome failed(String reason) {
        return new AuditOutcome(
                List.of(new CheckResult(
                        "fetch_failed",
                        AuditSeverity.CRITICAL,
                        0,
                        Map.of("reason", reason),
                        "Verify the URL is publicly reachable and uses HTTP or HTTPS.")),
                0,
                "Audit could not complete: " + reason);
    }

    private static boolean isHtml(FetchedPage page) {
        String ct = page.contentType().toLowerCase(Locale.ROOT);
        return ct.isBlank() || ct.contains("text/html") || ct.contains("application/xhtml");
    }

    private static List<CheckResult> htmlChecks(String html) {
        List<CheckResult> checks = new ArrayList<>();

        String title = firstGroup(TITLE, html);
        boolean titleOk = title != null && !title.isBlank();
        checks.add(new CheckResult(
                "title",
                AuditSeverity.WARNING,
                titleOk ? 100 : 0,
                titleOk ? Map.of("title", title.trim()) : Map.of(),
                titleOk ? null : "Add a unique, descriptive <title> under 60 characters."));

        Map<String, String> metas = metaAttributes(html);
        String description = metas.get("description");
        boolean descOk = description != null && !description.isBlank();
        checks.add(new CheckResult(
                "meta_description",
                AuditSeverity.WARNING,
                descOk ? 100 : 0,
                descOk ? Map.of("content", truncate(description, 200)) : Map.of(),
                descOk ? null : "Add a meta description (120-160 characters)."));

        String viewport = metas.get("viewport");
        boolean viewportOk = viewport != null && viewport.contains("width");
        checks.add(new CheckResult(
                "viewport",
                AuditSeverity.WARNING,
                viewportOk ? 100 : 0,
                viewportOk ? Map.of("content", viewport) : Map.of(),
                viewportOk ? null : "Add a viewport meta tag so the page renders well on mobile."));

        String ogTitle = metas.get("og:title");
        String ogDescription = metas.get("og:description");
        boolean ogOk = ogTitle != null && !ogTitle.isBlank() && ogDescription != null && !ogDescription.isBlank();
        checks.add(new CheckResult(
                "open_graph",
                AuditSeverity.INFO,
                ogOk ? 100 : 0,
                ogOk ? Map.of("ogTitle", truncate(ogTitle, 120)) : Map.of("missing", missingOg(ogTitle, ogDescription)),
                ogOk ? null : "Add og:title and og:description for rich social sharing."));

        boolean canonicalOk = hasLinkRelation(html, "canonical");
        checks.add(new CheckResult(
                "canonical",
                AuditSeverity.INFO,
                canonicalOk ? 100 : 0,
                Map.of(),
                canonicalOk ? null : "Add a canonical link tag to the preferred URL."));

        boolean h1Ok = H1.matcher(html).find();
        checks.add(new CheckResult(
                "h1_present",
                AuditSeverity.INFO,
                h1Ok ? 100 : 0,
                Map.of(),
                h1Ok ? null : "Add exactly one H1 describing the page."));

        checks.add(imageAltCheck(html));

        boolean charsetOk = html.toLowerCase(Locale.ROOT).contains("charset");
        checks.add(new CheckResult(
                "charset",
                AuditSeverity.INFO,
                charsetOk ? 100 : 0,
                Map.of(),
                charsetOk ? null : "Declare a character encoding, such as UTF-8."));

        Matcher htmlTag = HTML_TAG.matcher(html);
        String lang = htmlTag.find() ? attr(htmlTag.group(), "lang") : null;
        boolean langOk = lang != null;
        checks.add(new CheckResult(
                "html_lang",
                AuditSeverity.WARNING,
                langOk ? 100 : 0,
                langOk ? Map.of("lang", lang) : Map.of(),
                langOk ? null : "Add a lang attribute to the html element, such as lang=\"en\"."));

        return checks;
    }

    private static CheckResult imageAltCheck(String html) {
        Matcher matcher = IMG_TAG.matcher(html);
        int total = 0;
        int withAlt = 0;
        while (matcher.find()) {
            total++;
            String alt = attr(matcher.group(), "alt");
            if (alt != null && !alt.isBlank()) {
                withAlt++;
            }
        }
        if (total == 0) {
            return new CheckResult("img_alt", AuditSeverity.WARNING, 100, Map.of(), null);
        }
        int score = (int) Math.round(withAlt * 100.0 / total);
        return new CheckResult(
                "img_alt",
                AuditSeverity.WARNING,
                score,
                Map.of("images", String.valueOf(total), "withAlt", String.valueOf(withAlt)),
                score == 100 ? null : "Add descriptive alt text to all images (" + withAlt + "/" + total + " have it).");
    }

    private static Map<String, String> metaAttributes(String html) {
        Map<String, String> result = new java.util.HashMap<>();
        Matcher matcher = META_TAG.matcher(html);
        while (matcher.find()) {
            String tag = matcher.group();
            String name = firstNonNull(attr(tag, "name"), attr(tag, "property"));
            String content = attr(tag, "content");
            if (name != null && content != null) {
                result.putIfAbsent(name.toLowerCase(Locale.ROOT), content);
            }
        }
        return result;
    }

    private static boolean hasLinkRelation(String html, String relation) {
        Matcher matcher = LINK_TAG.matcher(html);
        while (matcher.find()) {
            String rel = attr(matcher.group(), "rel");
            if (rel != null && rel.toLowerCase(Locale.ROOT).contains(relation)) {
                return true;
            }
        }
        return false;
    }

    // ---- Site-level checks for bounded crawls (product spec §3.3) ----

    static final double SEED_WEIGHT = 0.4;
    static final double SITE_WEIGHT = 0.6;
    static final int THIN_CONTENT_WORDS = 250;

    /** Full audit plus site-level checks over crawled pages (seed-first). */
    public AuditOutcome auditWithSite(FetchedPage seed, FetchedPage robots, FetchedPage sitemap,
            List<FetchedPage> pages) {
        AuditOutcome base = audit(seed, robots, sitemap);
        if (pages == null || pages.size() <= 1) {
            return base;
        }
        List<CheckResult> checks = new ArrayList<>(base.checks());
        checks.addAll(siteChecks(pages));
        int overall = siteOverall(base.overallScore(), pages);
        String summary = base.summary() + " Site crawl: " + pages.size() + " pages.";
        return new AuditOutcome(checks, overall, summary);
    }

    static List<CheckResult> siteChecks(List<FetchedPage> pages) {
        List<CheckResult> checks = new ArrayList<>();
        int crawled = pages.size() - 1;
        checks.add(new CheckResult(
                "internal_links",
                AuditSeverity.INFO,
                Math.min(100, crawled * 20),
                Map.of("pagesCrawled", String.valueOf(crawled)),
                crawled >= 3 ? null : "Only " + crawled + " internal page(s) reachable — link more pages."));

        Map<String, Integer> titleCounts = new java.util.HashMap<>();
        int thin = 0;
        int htmlPages = 0;
        int missingAltPages = 0;
        for (int i = 1; i < pages.size(); i++) {
            FetchedPage page = pages.get(i);
            if (page.statusCode() < 200 || page.statusCode() >= 300 || !isHtmlPage(page)) {
                continue;
            }
            htmlPages++;
            String title = firstGroup(TITLE, page.body());
            if (title != null && !title.isBlank()) {
                titleCounts.merge(title.trim().toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
            if (wordCount(page.body()) < THIN_CONTENT_WORDS) {
                thin++;
            }
            Matcher imgs = IMG_TAG.matcher(page.body());
            while (imgs.find()) {
                String alt = attr(imgs.group(), "alt");
                if (alt == null || alt.isBlank()) {
                    missingAltPages++;
                    break;
                }
            }
        }

        int duplicateTitles = titleCounts.values().stream()
                .mapToInt(count -> Math.max(0, count - 1)).sum();
        checks.add(new CheckResult(
                "duplicate_titles",
                AuditSeverity.WARNING,
                Math.max(0, 100 - duplicateTitles * 25),
                Map.of("duplicateTitles", String.valueOf(duplicateTitles),
                        "htmlPages", String.valueOf(htmlPages)),
                duplicateTitles == 0 ? null : "Give each crawled page a unique <title>."));

        checks.add(new CheckResult(
                "thin_content",
                AuditSeverity.WARNING,
                htmlPages == 0 ? 100 : Math.max(0, 100 - thin * 20),
                Map.of("thinPages", String.valueOf(thin),
                        "thresholdWords", String.valueOf(THIN_CONTENT_WORDS)),
                thin == 0 ? null : "Expand " + thin + " thin page(s) beyond " + THIN_CONTENT_WORDS + " words."));

        checks.add(new CheckResult(
                "missing_alt_by_page",
                AuditSeverity.WARNING,
                Math.max(0, 100 - missingAltPages * 15),
                Map.of("pagesMissingAlt", String.valueOf(missingAltPages)),
                missingAltPages == 0 ? null
                        : "Add alt text on " + missingAltPages + " page(s) with images lacking alt."));

        return checks;
    }

    /** Site score = weighted mean of seed score and mean per-page content score. */
    static int siteOverall(int seedScore, List<FetchedPage> pages) {
        List<Integer> pageScores = new ArrayList<>();
        for (int i = 1; i < pages.size(); i++) {
            int score = contentScore(pages.get(i));
            if (score >= 0) {
                pageScores.add(score);
            }
        }
        if (pageScores.isEmpty()) {
            return seedScore;
        }
        double mean = pageScores.stream().mapToInt(Integer::intValue).average().orElse(seedScore);
        return (int) Math.round(SEED_WEIGHT * seedScore + SITE_WEIGHT * mean);
    }

    /** Per-page content score from title/meta/h1/alt; -1 when not scorable. */
    static int contentScore(FetchedPage page) {
        if (page.statusCode() < 200 || page.statusCode() >= 300 || !isHtmlPage(page)) {
            return -1;
        }
        String html = page.body();
        int score = 0;
        String title = firstGroup(TITLE, html);
        if (title != null && !title.isBlank()) {
            score += 25;
        }
        Map<String, String> meta = metaAttributes(html);
        if (meta.containsKey("description") && !meta.get("description").isBlank()) {
            score += 25;
        }
        if (H1.matcher(html).find()) {
            score += 25;
        }
        int total = 0;
        int withAlt = 0;
        Matcher imgs = IMG_TAG.matcher(html);
        while (imgs.find()) {
            total++;
            String alt = attr(imgs.group(), "alt");
            if (alt != null && !alt.isBlank()) {
                withAlt++;
            }
        }
        score += total == 0 ? 25 : (int) Math.round(withAlt * 25.0 / total);
        return score;
    }

    private static int wordCount(String html) {
        String text = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?s)<[^>]+>", " ");
        if (text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private static boolean isHtmlPage(FetchedPage page) {
        return page.contentType() == null
                || page.contentType().toLowerCase(Locale.ROOT).contains("html");
    }

    /** Site score = weighted mean of seed score and mean per-page content score. */
    static int siteOverall(int seedScore, List<FetchedPage> pages) {
        List<Integer> pageScores = new ArrayList<>();
        for (int i = 1; i < pages.size(); i++) {
            int score = contentScore(pages.get(i));
            if (score >= 0) {
                pageScores.add(score);
            }
        }
        if (pageScores.isEmpty()) {
            return seedScore;
        }
        double mean = pageScores.stream().mapToInt(Integer::intValue).average().orElse(seedScore);
        return (int) Math.round(SEED_WEIGHT * seedScore + SITE_WEIGHT * mean);
    }

    /** Per-page content score from title/meta/h1/alt; -1 when not scorable. */
    static int contentScore(FetchedPage page) {
        if (page.statusCode() < 200 || page.statusCode() >= 300 || !isHtmlPage(page)) {
            return -1;
        }
        String html = page.body();
        int score = 0;
        String title = firstGroup(TITLE, html);
        if (title != null && !title.isBlank()) {
            score += 25;
        }
        Map<String, String> meta = metaAttributes(html);
        if (meta.containsKey("description") && !meta.get("description").isBlank()) {
            score += 25;
        }
        if (H1.matcher(html).find()) {
            score += 25;
        }
        int total = 0;
        int withAlt = 0;
        Matcher imgs = IMG_TAG.matcher(html);
        while (imgs.find()) {
            total++;
            String alt = attr(imgs.group(), "alt");
            if (alt != null && !alt.isBlank()) {
                withAlt++;
            }
        }
        score += total == 0 ? 25 : (int) Math.round(withAlt * 25.0 / total);
        return score;
    }

    private static int wordCount(String html) {
        String text = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?s)<[^>]+>", " ");
        if (text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private static boolean isHtmlPage(FetchedPage page) {
        return page.contentType() == null
                || page.contentType().toLowerCase(Locale.ROOT).contains("html");
    }

    static String attr(String tag, String name) {
        Pattern pattern = Pattern.compile(
                "(?i)(?:^|\\s)" + Pattern.quote(name) + "\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");
        Matcher matcher = pattern.matcher(tag);
        if (!matcher.find()) {
            return null;
        }
        return firstNonNull(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    private static String firstGroup(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String missingOg(String ogTitle, String ogDescription) {
        List<String> missing = new ArrayList<>();
        if (ogTitle == null || ogTitle.isBlank()) {
            missing.add("og:title");
        }
        if (ogDescription == null || ogDescription.isBlank()) {
            missing.add("og:description");
        }
        return String.join(", ", missing);
    }

    private static String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "...";
    }
}
