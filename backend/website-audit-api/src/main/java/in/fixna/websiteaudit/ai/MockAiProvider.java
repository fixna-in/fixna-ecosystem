package in.fixna.websiteaudit.ai;

import in.fixna.websiteaudit.ai.dto.AiSuggestionResponse;
import in.fixna.websiteaudit.ai.dto.WebsiteAuditFixSuggestion;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Deterministic advisory AI provider. Active in every profile (including prod)
 * so {@code AiProvider} is always injectable — matches consulting's MockAiProvider.
 * Returns placeholder suggestions only; no external calls.
 */
@Component
public class MockAiProvider implements AiProvider {

    @Override
    public WebsiteAuditFixSuggestion suggestWebsiteAuditFixes(String url, List<String> findings) {
        if (findings == null || findings.isEmpty()) {
            return new WebsiteAuditFixSuggestion(url, List.of("No findings — nothing to fix."));
        }
        return new WebsiteAuditFixSuggestion(url, List.of(
                "1. Address critical findings first (HTTPS, HTTP status).",
                "2. Add missing meta tags (title, description, viewport, Open Graph).",
                "3. Ensure all images have descriptive alt text.",
                "4. Add canonical and hreflang links where appropriate.",
                "5. Improve response time via caching and CDN."
        ));
    }
}