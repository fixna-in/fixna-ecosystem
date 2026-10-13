package in.fixna.websiteaudit.ai;

import java.util.List;

import in.fixna.websiteaudit.ai.dto.WebsiteAuditFixSuggestion;

/** Advisory suggestion provider for Website Audit — no autonomous actions. */
public interface AiProvider {

    /** Advisory prioritized fixes derived from deterministic audit findings. */
    WebsiteAuditFixSuggestion suggestWebsiteAuditFixes(String url, List<String> findings);
}