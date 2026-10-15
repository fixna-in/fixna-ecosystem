package in.fixna.websiteaudit.check;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import in.fixna.websiteaudit.AuditSeverity;
import in.fixna.websiteaudit.check.WebsiteAuditor.AuditOutcome;
import in.fixna.websiteaudit.check.WebsiteAuditor.CheckResult;
import in.fixna.websiteaudit.fetcher.WebsiteFetcher.FetchedPage;

import static org.assertj.core.api.Assertions.assertThat;

class WebsiteAuditorTest {

    private final WebsiteAuditor auditor = new WebsiteAuditor();

    private static FetchedPage htmlPage(String html) {
        return new FetchedPage(
                URI.create("https://example.com/"),
                URI.create("https://example.com/"),
                200,
                800,
                "text/html; charset=UTF-8",
                html);
    }

    private static FetchedPage auxiliary(String path, int status) {
        return new FetchedPage(
                URI.create("https://example.com" + path),
                URI.create("https://example.com" + path),
                status,
                50,
                "text/plain",
                "");
    }

    private static Map<String, String> scoreByCode(AuditOutcome outcome) {
        return outcome.checks().stream()
                .collect(java.util.stream.Collectors.toMap(CheckResult::code, c -> String.valueOf(c.score())));
    }

    @Test
    void wellOptimizedPageScoresFull() {
        String html = """
                <html lang="en"><head>
                <meta charset="UTF-8">
                <title>Fixna Consulting - Strategy that ships</title>
                <meta name="description" content="Consulting that helps teams deliver measurable outcomes.">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta property="og:title" content="Fixna Consulting">
                <meta property="og:description" content="Strategy that ships">
                <link rel="canonical" href="https://example.com/">
                </head><body><h1>Welcome</h1><img src="team.png" alt="Our team"></body></html>
                """;

        AuditOutcome outcome = auditor.audit(htmlPage(html), auxiliary("/robots.txt", 200), auxiliary("/sitemap.xml", 200));

        assertThat(outcome.overallScore()).isEqualTo(100);
        assertThat(outcome.checks()).allMatch(c -> c.score() == 100);
        assertThat(outcome.summary()).contains("checks passed").contains("100/100");
    }

    @Test
    void missingTagsFailDeterministically() {
        String html = "<html><body><p>Hello</p></body></html>";

        AuditOutcome outcome = auditor.audit(htmlPage(html), null, null);
        Map<String, String> scores = scoreByCode(outcome);

        assertThat(scores.get("title")).isEqualTo("0");
        assertThat(scores.get("meta_description")).isEqualTo("0");
        assertThat(scores.get("viewport")).isEqualTo("0");
        assertThat(scores.get("open_graph")).isEqualTo("0");
        assertThat(scores.get("h1_present")).isEqualTo("0");
        assertThat(scores.get("robots_txt")).isEqualTo("0");
        assertThat(scores.get("sitemap_xml")).isEqualTo("0");
        assertThat(scores.get("html_lang")).isEqualTo("0");
        assertThat(outcome.overallScore()).isLessThan(70);

        CheckResult title = outcome.checks().stream()
                .filter(c -> c.code().equals("title"))
                .findFirst()
                .orElseThrow();
        assertThat(title.fixHint()).contains("title");
    }

    @Test
    void nonHtmlContentSkipsHtmlChecks() {
        FetchedPage page = new FetchedPage(
                URI.create("https://example.com/api"),
                URI.create("https://example.com/api"),
                200,
                300,
                "application/json",
                "{\"ok\":true}");

        AuditOutcome outcome = auditor.audit(page, null, null);

        assertThat(outcome.checks())
                .extracting(CheckResult::code)
                .contains("html_content")
                .doesNotContain("title", "meta_description");
        assertThat(scoreByCode(outcome).get("http_status")).isEqualTo("100");
    }

    @Test
    void imageAltScoreIsPartialWhenSomeImagesMissingAlt() {
        String html = """
                <html lang="en"><head><title>T</title></head><body>
                <img src="a.png" alt="first"><img src="b.png"><img src="c.png"><img src="d.png" alt="fourth">
                </body></html>
                """;

        AuditOutcome outcome = auditor.audit(htmlPage(html), null, null);

        CheckResult imgAlt = outcome.checks().stream()
                .filter(c -> c.code().equals("img_alt"))
                .findFirst()
                .orElseThrow();
        assertThat(imgAlt.score()).isEqualTo(50);
        assertThat(imgAlt.evidence()).containsEntry("images", "4").containsEntry("withAlt", "2");
    }

    @Test
    void failedOutcomeIsCriticalAndScoreZero() {
        AuditOutcome outcome = WebsiteAuditor.failed("target unreachable or blocked");

        assertThat(outcome.overallScore()).isZero();
        assertThat(outcome.checks()).hasSize(1);
        assertThat(outcome.checks().getFirst().code()).isEqualTo("fetch_failed");
        assertThat(outcome.checks().getFirst().severity()).isEqualTo(AuditSeverity.CRITICAL);
        assertThat(outcome.summary()).contains("could not complete");
    }

    @Test
    void attrReadsSingleQuotedAndUnquotedValues() {
        assertThat(WebsiteAuditor.attr("<meta name='viewport' content=\"a b\">", "name")).isEqualTo("viewport");
        assertThat(WebsiteAuditor.attr("<html lang=en>", "lang")).isEqualTo("en");
        assertThat(WebsiteAuditor.attr("<meta content=\"x\" name=\"d\">", "content")).isEqualTo("x");
    }
}
