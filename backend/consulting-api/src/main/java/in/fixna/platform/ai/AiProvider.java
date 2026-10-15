package in.fixna.platform.ai;

import in.fixna.platform.ai.dto.ClientSummarySuggestion;
import in.fixna.platform.ai.dto.MeetingAgendaSuggestion;
import in.fixna.platform.ai.dto.ProposalSuggestion;
import in.fixna.platform.ai.dto.WebsiteAuditFixSuggestion;
import in.fixna.platform.ai.dto.InvoiceLineIdeasSuggestion;
import in.fixna.platform.ai.dto.WebsiteCopySuggestion;
import in.fixna.platform.consulting.client.Client;
import in.fixna.platform.consulting.profile.ConsultingProfile;

/** Advisory suggestion provider — no autonomous actions. */
public interface AiProvider {

    ProposalSuggestion suggestProposalDraft(Client client, String topic);

    MeetingAgendaSuggestion suggestMeetingAgenda(String title, Client client);

    ClientSummarySuggestion suggestClientSummary(Client client);

    WebsiteCopySuggestion suggestWebsiteCopy(ConsultingProfile profile);

    /** Advisory prioritized fixes derived from deterministic audit findings. */
    WebsiteAuditFixSuggestion suggestWebsiteAuditFixes(String url, java.util.List<String> findings);

    /** Advisory line-item ideas for an invoice; never applied without review. */
    InvoiceLineIdeasSuggestion suggestInvoiceLineIdeas(String invoiceTitle, java.util.List<String> existingLines);
}
