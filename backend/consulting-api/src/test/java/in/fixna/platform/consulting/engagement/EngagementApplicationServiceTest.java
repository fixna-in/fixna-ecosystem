package in.fixna.platform.consulting.engagement;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.client.Client;
import in.fixna.platform.consulting.client.ClientRepository;
import in.fixna.platform.consulting.dto.StatusTransitionRequest;
import in.fixna.platform.consulting.engagement.dto.EngagementRequest;
import in.fixna.platform.consulting.engagement.dto.EngagementResponse;
import in.fixna.platform.event.DomainEventPublisher;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngagementApplicationServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    @Mock EngagementRepository engagements;
    @Mock ClientRepository clients;
    @Mock DomainEventPublisher events;
    @Mock AuditPublisher audit;

    @InjectMocks EngagementApplicationService service;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private void asConsultantAdmin() {
        TenantContext.set(tenantId, userId, Role.CONSULTANT_ADMIN);
    }

    private void asClientUser() {
        TenantContext.set(tenantId, userId, Role.CLIENT_USER);
    }

    @Test
    void createPersistsEngagementAndPublishesEvent() {
        asConsultantAdmin();
        Client client = new Client();
        client.setId(clientId);
        client.setTenantId(tenantId);
        when(clients.findByIdAndTenantId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(engagements.save(any(Engagement.class))).thenAnswer(invocation -> {
            Engagement engagement = invocation.getArgument(0);
            engagement.setId(UUID.randomUUID());
            return engagement;
        });

        EngagementResponse response = service.create(
                clientId, new EngagementRequest("Q1 Advisory", "Scope notes", null, null));

        assertThat(response.title()).isEqualTo("Q1 Advisory");
        assertThat(response.clientId()).isEqualTo(clientId);
        assertThat(response.status()).isEqualTo(EngagementStatus.DRAFT);
        verify(events).publish(any());
        verify(audit).publish(any());
    }

    @Test
    void transitionMovesEngagementToActive() {
        asConsultantAdmin();
        UUID engagementId = UUID.randomUUID();
        Engagement existing = new Engagement();
        existing.setId(engagementId);
        existing.setTenantId(tenantId);
        existing.setClientId(clientId);
        existing.setTitle("Engagement");
        existing.setStatus(EngagementStatus.DRAFT);
        when(engagements.findByIdAndTenantId(engagementId, tenantId)).thenReturn(Optional.of(existing));
        when(engagements.save(any(Engagement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EngagementResponse response = service.transition(engagementId, new StatusTransitionRequest("ACTIVE"));

        assertThat(response.status()).isEqualTo(EngagementStatus.ACTIVE);
        verify(events).publish(any());
    }

    @Test
    void clientUserCannotCreateEngagement() {
        asClientUser();

        assertThatThrownBy(() -> service.create(clientId, new EngagementRequest("Blocked", null, null, null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void missingEngagementReturnsNotFound() {
        asConsultantAdmin();
        UUID engagementId = UUID.randomUUID();
        when(engagements.findByIdAndTenantId(engagementId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(engagementId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("NOT_FOUND");
    }
}
