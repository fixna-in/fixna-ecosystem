package in.fixna.platform.ai;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import in.fixna.platform.ai.dto.ClientSummarySuggestion;
import in.fixna.platform.ai.dto.InvoiceLineIdeasSuggestion;
import in.fixna.platform.ai.dto.MeetingAgendaSuggestion;
import in.fixna.platform.ai.dto.ProposalSuggestion;
import in.fixna.platform.ai.dto.WebsiteAuditFixSuggestion;
import in.fixna.platform.ai.dto.WebsiteCopySuggestion;
import in.fixna.platform.consulting.client.Client;
import in.fixna.platform.consulting.profile.ConsultingProfile;

import static org.assertj.core.api.Assertions.assertThat;

class MockAiProviderTest {

    private final MockAiProvider provider = new MockAiProvider();

    @Test
    void suggestProposalDraftIsDeterministic() {
        Client client = client("Acme Corp", "Technology", null);

        ProposalSuggestion first = provider.suggestProposalDraft(client, "Digital transformation");
        ProposalSuggestion second = provider.suggestProposalDraft(client, "Digital transformation");

        assertThat(first.title()).isEqualTo("Proposal for Acme Corp: Digital transformation");
        assertThat(first.lineItems()).hasSize(3);
        assertThat(first.lineItems().get(0).description()).contains("Discovery");
        assertThat(first).isEqualTo(second);
    }

    @Test
    void suggestProposalDraftUsesDefaultTopicWhenBlank() {
        Client client = client("Beta LLC", null, null);

        ProposalSuggestion suggestion = provider.suggestProposalDraft(client, "  ");

        assertThat(suggestion.title()).isEqualTo("Proposal for Beta LLC: Consulting engagement");
    }

    @Test
    void suggestMeetingAgendaIsDeterministic() {
        Client client = client("Acme Corp", null, null);

        MeetingAgendaSuggestion first = provider.suggestMeetingAgenda("Quarterly review", client);
        MeetingAgendaSuggestion second = provider.suggestMeetingAgenda("Quarterly review", client);

        assertThat(first.bullets()).hasSize(4);
        assertThat(first.bullets().get(0)).contains("Quarterly review");
        assertThat(first.bullets().get(1)).contains("Acme Corp");
        assertThat(first).isEqualTo(second);
    }

    @Test
    void suggestMeetingAgendaWithoutClientUsesStakeholders() {
        MeetingAgendaSuggestion suggestion = provider.suggestMeetingAgenda("Standup", null);

        assertThat(suggestion.bullets().get(1)).contains("stakeholders");
    }

    @Test
    void suggestClientSummaryIsDeterministic() {
        Client client = client("Gamma Inc", "Healthcare", "Expanding into new markets");

        ClientSummarySuggestion first = provider.suggestClientSummary(client);
        ClientSummarySuggestion second = provider.suggestClientSummary(client);

        assertThat(first.summary()).contains("Gamma Inc").contains("Healthcare").contains("Expanding");
        assertThat(first.nextSteps()).hasSize(3);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void suggestWebsiteCopyUsesProfileWhenPresent() {
        ConsultingProfile profile = new ConsultingProfile();
        profile.setId(UUID.randomUUID());
        profile.setTenantId(UUID.randomUUID());
        profile.setDisplayName("Fixna Advisors");
        profile.setTagline("Strategy that ships");
        profile.setBio("We help teams deliver.");

        WebsiteCopySuggestion suggestion = provider.suggestWebsiteCopy(profile);

        assertThat(suggestion.tagline()).isEqualTo("Strategy that ships");
        assertThat(suggestion.bio()).isEqualTo("We help teams deliver.");
    }

    @Test
    void suggestWebsiteCopyFallsBackWhenProfileEmpty() {
        ConsultingProfile profile = new ConsultingProfile();
        profile.setId(UUID.randomUUID());
        profile.setTenantId(UUID.randomUUID());

        WebsiteCopySuggestion suggestion = provider.suggestWebsiteCopy(profile);

        assertThat(suggestion.tagline()).contains("Your consulting practice");
        assertThat(suggestion.bio()).contains("helps organizations");
    }

    @Test
    void suggestWebsiteAuditFixesIsDeterministicAndCappedAtSix() {
        List<String> findings = List.of("title: Add a title", "viewport: Add viewport");

        WebsiteAuditFixSuggestion first = provider.suggestWebsiteAuditFixes("https://example.com/", findings);
        WebsiteAuditFixSuggestion second = provider.suggestWebsiteAuditFixes("https://example.com/", findings);

        assertThat(first.url()).isEqualTo("https://example.com/");
        assertThat(first.priorities()).hasSize(5);
        assertThat(first.priorities().get(0)).isEqualTo("Address finding: title: Add a title");
        assertThat(first).isEqualTo(second);

        List<String> many = List.of("a", "b", "c", "d", "e", "f", "g");
        assertThat(provider.suggestWebsiteAuditFixes("https://example.com/", many).priorities()).hasSize(6);
        assertThat(provider.suggestWebsiteAuditFixes("https://example.com/", List.of()).priorities()).hasSize(3);
    }

    @Test
    void suggestInvoiceLineIdeasIsDeterministicAndExcludesExisting() {
        InvoiceLineIdeasSuggestion first = provider.suggestInvoiceLineIdeas("Website redesign", List.of());
        InvoiceLineIdeasSuggestion second = provider.suggestInvoiceLineIdeas("Website redesign", List.of());

        assertThat(first.ideas()).hasSize(3);
        assertThat(first.ideas().get(0)).contains("Website redesign");
        assertThat(first).isEqualTo(second);

        InvoiceLineIdeasSuggestion withExisting = provider.suggestInvoiceLineIdeas("Website redesign", first.ideas());
        assertThat(withExisting.ideas()).hasSize(1);
        assertThat(withExisting.ideas().get(0)).isEqualTo("Advisory retainer follow-up support");

        InvoiceLineIdeasSuggestion blankTitle = provider.suggestInvoiceLineIdeas("  ", List.of());
        assertThat(blankTitle.ideas().get(0)).contains("this engagement");
    }

    private static Client client(String name, String industry, String notes) {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setTenantId(UUID.randomUUID());
        client.setName(name);
        client.setIndustry(industry);
        client.setNotes(notes);
        return client;
    }
}
