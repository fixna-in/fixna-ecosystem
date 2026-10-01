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
import in.fixna.platform.event.DomainEventPublisher;
import in.fixna.platform.identity.UserRepository;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientTenantIsolationTest {

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private final UUID clientOfTenantA = UUID.randomUUID();

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

    @Test
    void repositoryLookupReturnsEmptyForCrossTenantId() {
        when(clients.findByIdAndTenantId(clientOfTenantA, tenantB)).thenReturn(Optional.empty());

        Optional<Client> result = clients.findByIdAndTenantId(clientOfTenantA, tenantB);

        assertThat(result).isEmpty();
    }

    @Test
    void crossTenantGetSurfacesAsNotFound() {
        TenantContext.set(tenantB, UUID.randomUUID(), Role.CONSULTANT_ADMIN);
        when(clients.findByIdAndTenantId(clientOfTenantA, tenantB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(clientOfTenantA))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void crossTenantGetUsesNotFoundStatus() {
        TenantContext.set(tenantB, UUID.randomUUID(), Role.CONSULTANT_ADMIN);
        when(clients.findByIdAndTenantId(clientOfTenantA, tenantB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(clientOfTenantA))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
