package in.fixna.platform.ai.dto;

import java.util.List;

/** Advisory prioritized audit fixes. Never applied automatically. */
public record WebsiteAuditFixSuggestion(String url, List<String> priorities) {}
