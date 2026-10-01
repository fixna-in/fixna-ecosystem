package in.fixna.platform.consulting.client;

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
import in.fixna.platform.consulting.client.dto.ClientRequest;
import in.fixna.platform.consulting.client.dto.ClientResponse;
import in.fixna.platform.event.DomainEventPublisher;
import in.fixna.platform.identity.UserRepository;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientApplicationServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Mock ClientRepository clients;
    @Mock ClientContactRepository contacts;
    @Mock ClientPortalMembershipRepository portalMemberships;
    @Mock UserRepository users;
    @Mock DomainEventPublisher events;
    @Mock AuditPublisher audit;

    @InjectMocks ClientApplicationService service;

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
    void createPersistsClientAndPublishesEvent() {
        asConsultantAdmin();
        when(clients.save(any(Client.class))).thenAnswer(invocation -> {
            Client client = invocation.getArgument(0);
            client.setId(UUID.randomUUID());
            return client;
        });

        ClientResponse response = service.create(
                new ClientRequest("Acme Corp", "ops@acme.test", "+1-555-0100", null, "Technology", null));

        assertThat(response.name()).isEqualTo("Acme Corp");
        assertThat(response.email()).isEqualTo("ops@acme.test");
        verify(events).publish(any());
        verify(audit).publish(any());
    }

    @Test
    void updateChangesClientFields() {
        asConsultantAdmin();
        UUID clientId = UUID.randomUUID();
        Client existing = new Client();
        existing.setId(clientId);
        existing.setTenantId(tenantId);
        existing.setName("Old Name");
        when(clients.findByIdAndTenantId(clientId, tenantId)).thenReturn(Optional.of(existing));
        when(clients.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientResponse response = service.update(
                clientId, new ClientRequest("New Name", "new@acme.test", null, null, null, "Updated notes"));

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.notes()).isEqualTo("Updated notes");
        verify(events).publish(any());
    }

    @Test
    void clientUserCannotCreateClient() {
        asClientUser();

        assertThatThrownBy(() -> service.create(new ClientRequest("Blocked", null, null, null, null, null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void clientUserCannotUpdateClient() {
        asClientUser();

        assertThatThrownBy(() -> service.update(
                        UUID.randomUUID(), new ClientRequest("Blocked", null, null, null, null, null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void missingClientReturnsNotFound() {
        asConsultantAdmin();
        UUID clientId = UUID.randomUUID();
        when(clients.findByIdAndTenantId(clientId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(clientId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void updateMissingClientReturnsNotFound() {
        asConsultantAdmin();
        UUID clientId = UUID.randomUUID();
        when(clients.findByIdAndTenantId(clientId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                        clientId, new ClientRequest("Missing", null, null, null, null, null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("NOT_FOUND");
    }
}
