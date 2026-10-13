package in.fixna.platform.ai.dto;

import java.util.List;

/** Advisory invoice line-item ideas. Never applied automatically. */
public record InvoiceLineIdeasSuggestion(List<String> ideas) {}
