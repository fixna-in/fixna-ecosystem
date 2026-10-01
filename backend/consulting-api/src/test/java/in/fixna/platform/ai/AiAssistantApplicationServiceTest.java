package in.fixna.platform.ai;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.ai.dto.AiSuggestionResponse;
import in.fixna.platform.ai.dto.ClientSummarySuggestion;
import in.fixna.platform.ai.dto.MeetingAgendaSuggestRequest;
import in.fixna.platform.ai.dto.ProposalSuggestRequest;
import in.fixna.platform.ai.dto.ProposalSuggestion;
import in.fixna.platform.consulting.client.Client;
import in.fixna.platform.consulting.client.ClientRepository;
import in.fixna.platform.consulting.profile.ConsultingProfileRepository;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAssistantApplicationServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    @Mock AiProvider aiProvider;
    @Mock AiUsageLogRepository usageLogs;
    @Mock ClientRepository clients;
    @Mock ConsultingProfileRepository profiles;
    @Mock AuditPublisher audit;

    @InjectMocks AiAssistantApplicationService service;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private void asConsultant() {
        TenantContext.set(tenantId, userId, Role.CONSULTANT);
    }

    private void asClientUser() {
        TenantContext.set(tenantId, userId, Role.CLIENT_USER);
    }

    @Test
    void suggestProposalPersistsUsageLogAndAudit() {
        asConsultant();
        Client client = client(clientId, tenantId, "Acme");
        when(clients.findByIdAndTenantId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(aiProvider.suggestProposalDraft(client, "Strategy"))
                .thenReturn(new ProposalSuggestion("Title", List.of()));
        when(usageLogs.save(any(AiUsageLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiSuggestionResponse<ProposalSuggestion> response =
                service.suggestProposal(new ProposalSuggestRequest(clientId, "Strategy"));

        assertThat(response.disclaimer()).isEqualTo(AiSuggestionResponse.ADVISORY_DISCLAIMER);
        assertThat(response.suggestion().title()).isEqualTo("Title");

        ArgumentCaptor<AiUsageLog> logCaptor = ArgumentCaptor.forClass(AiUsageLog.class);
        verify(usageLogs).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(logCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(logCaptor.getValue().getFeature()).isEqualTo(AiFeature.PROPOSAL_DRAFT);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).publish(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("ai.suggestion.generated");
    }

    @Test
    void clientUserCannotUseAi() {
        asClientUser();

        assertThatThrownBy(() -> service.suggestProposal(new ProposalSuggestRequest(clientId, null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crossTenantClientReturnsNotFound() {
        asConsultant();
        when(clients.findByIdAndTenantId(clientId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.summarizeClient(clientId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void summarizeClientReturnsSuggestionForTenantClient() {
        asConsultant();
        Client client = client(clientId, tenantId, "Acme");
        when(clients.findByIdAndTenantId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(aiProvider.suggestClientSummary(client))
                .thenReturn(new ClientSummarySuggestion("Summary", List.of("Step 1")));
        when(usageLogs.save(any(AiUsageLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiSuggestionResponse<ClientSummarySuggestion> response = service.summarizeClient(clientId);

        assertThat(response.suggestion().summary()).isEqualTo("Summary");
        verify(usageLogs).save(any(AiUsageLog.class));
    }

    @Test
    void suggestMeetingAgendaWithOptionalClient() {
        asConsultant();
        when(aiProvider.suggestMeetingAgenda("Kickoff", null))
                .thenReturn(new in.fixna.platform.ai.dto.MeetingAgendaSuggestion(List.of("Intro")));
        when(usageLogs.save(any(AiUsageLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.suggestMeetingAgenda(new MeetingAgendaSuggestRequest("Kickoff", null));

        assertThat(response.suggestion().bullets()).containsExactly("Intro");
    }

    private static Client client(UUID id, UUID tenantId, String name) {
        Client client = new Client();
        client.setId(id);
        client.setTenantId(tenantId);
        client.setName(name);
        return client;
    }

}
