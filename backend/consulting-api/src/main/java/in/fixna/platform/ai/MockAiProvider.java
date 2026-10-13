package in.fixna.platform.ai;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import in.fixna.platform.ai.dto.ClientSummarySuggestion;
import in.fixna.platform.ai.dto.MeetingAgendaSuggestion;
import in.fixna.platform.ai.dto.ProposalSuggestion;
import in.fixna.platform.ai.dto.ProposalSuggestion.ProposalLineItemSuggestion;
import in.fixna.platform.ai.dto.WebsiteAuditFixSuggestion;
import in.fixna.platform.ai.dto.InvoiceLineIdeasSuggestion;
import in.fixna.platform.ai.dto.WebsiteCopySuggestion;
import in.fixna.platform.consulting.client.Client;
import in.fixna.platform.consulting.profile.ConsultingProfile;

/**
 * Deterministic template provider for advisory suggestions.
 * No external API calls — safe for local development and tests.
 */
@Component
public class MockAiProvider implements AiProvider {

    @Override
    public ProposalSuggestion suggestProposalDraft(Client client, String topic) {
        String subject = topic != null && !topic.isBlank() ? topic.trim() : "Consulting engagement";
        String title = "Proposal for " + client.getName() + ": " + subject;
        return new ProposalSuggestion(
                title,
                List.of(
                        new ProposalLineItemSuggestion("Discovery and requirements analysis", BigDecimal.ONE, new BigDecimal("2500.00")),
                        new ProposalLineItemSuggestion("Implementation and delivery — " + subject, BigDecimal.ONE, new BigDecimal("7500.00")),
                        new ProposalLineItemSuggestion("Knowledge transfer and wrap-up", BigDecimal.ONE, new BigDecimal("1500.00"))));
    }

    @Override
    public MeetingAgendaSuggestion suggestMeetingAgenda(String title, Client client) {
        String clientLabel = client != null ? client.getName() : "stakeholders";
        return new MeetingAgendaSuggestion(List.of(
                "Welcome and objectives — " + title,
                "Review current status with " + clientLabel,
                "Discuss priorities, blockers, and decisions",
                "Agree on action items and next meeting"));
    }

    @Override
    public ClientSummarySuggestion suggestClientSummary(Client client) {
        String industry = client.getIndustry() != null ? client.getIndustry() : "their industry";
        String summary = client.getName()
                + " is an active client in "
                + industry
                + ". "
                + (client.getNotes() != null && !client.getNotes().isBlank()
                        ? "Notes: " + client.getNotes().trim()
                        : "No additional notes on file.");
        return new ClientSummarySuggestion(
                summary,
                List.of(
                        "Schedule a check-in to confirm current priorities",
                        "Review open proposals and project milestones",
                        "Identify upsell or expansion opportunities"));
    }

    @Override
    public WebsiteCopySuggestion suggestWebsiteCopy(ConsultingProfile profile) {
        String displayName = profile.getDisplayName() != null && !profile.getDisplayName().isBlank()
                ? profile.getDisplayName().trim()
                : "Your consulting practice";
        String existingTagline = profile.getTagline();
        String tagline = existingTagline != null && !existingTagline.isBlank()
                ? existingTagline.trim()
                : "Trusted expertise for " + displayName;
        String existingBio = profile.getBio();
        String bio = existingBio != null && !existingBio.isBlank()
                ? existingBio.trim()
                : displayName
                        + " helps organizations solve complex challenges with practical, results-driven consulting.";
        return new WebsiteCopySuggestion(tagline, bio);
    }

    @Override
    public WebsiteAuditFixSuggestion suggestWebsiteAuditFixes(String url, List<String> findings) {
        List<String> priorities = new java.util.ArrayList<>();
        for (String finding : findings) {
            if (priorities.size() >= 6) {
                break;
            }
            priorities.add("Address finding: " + finding);
        }
        List<String> generic = List.of(
                "Publish a sitemap.xml and reference it from robots.txt",
                "Add a clear call-to-action above the fold",
                "Review page copy so each key page targets one search intent");
        for (String item : generic) {
            if (priorities.size() >= 6) {
                break;
            }
            priorities.add(item);
        }
        return new WebsiteAuditFixSuggestion(url, List.copyOf(priorities));
    }

    @Override
    public InvoiceLineIdeasSuggestion suggestInvoiceLineIdeas(String invoiceTitle, List<String> existingLines) {
        String subject = invoiceTitle != null && !invoiceTitle.isBlank() ? invoiceTitle.trim() : "this engagement";
        List<String> candidates = List.of(
                "Discovery and scoping workshop — " + subject,
                "Implementation and delivery milestones — " + subject,
                "Handover, documentation, and training — " + subject,
                "Advisory retainer follow-up support");
        List<String> ideas = new java.util.ArrayList<>();
        for (String candidate : candidates) {
            if (existingLines.contains(candidate)) {
                continue;
            }
            ideas.add(candidate);
            if (ideas.size() == 3) {
                break;
            }
        }
        return new InvoiceLineIdeasSuggestion(List.copyOf(ideas));
    }
}
