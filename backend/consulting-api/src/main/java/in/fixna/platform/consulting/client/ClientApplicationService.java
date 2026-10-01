package in.fixna.platform.consulting.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.client.dto.ClientContactRequest;
import in.fixna.platform.consulting.client.dto.ClientContactResponse;
import in.fixna.platform.consulting.client.dto.ClientPortalMembershipRequest;
import in.fixna.platform.consulting.client.dto.ClientPortalMembershipResponse;
import in.fixna.platform.consulting.client.dto.ClientRequest;
import in.fixna.platform.consulting.client.dto.ClientResponse;
import in.fixna.platform.consulting.client.event.ClientCreated;
import in.fixna.platform.consulting.client.event.ClientUpdated;
import in.fixna.platform.event.DomainEventPublisher;
import in.fixna.platform.identity.UserRepository;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.TenantContext;

@Service
public class ClientApplicationService {

    private final ClientRepository clients;
    private final ClientContactRepository contacts;
    private final ClientPortalMembershipRepository portalMemberships;
    private final UserRepository users;
    private final DomainEventPublisher events;
    private final AuditPublisher audit;

    public ClientApplicationService(
            ClientRepository clients,
            ClientContactRepository contacts,
            ClientPortalMembershipRepository portalMemberships,
            UserRepository users,
            DomainEventPublisher events,
            AuditPublisher audit) {
        this.clients = clients;
        this.contacts = contacts;
        this.portalMemberships = portalMemberships;
        this.users = users;
        this.events = events;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> list() {
        TenantContext.requirePermission(Permission.CLIENT_VIEW);
        UUID tenantId = TenantContext.requireTenantId();
        return clients.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(ClientResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse get(UUID id) {
        TenantContext.requirePermission(Permission.CLIENT_VIEW);
        return ClientResponse.from(requireClient(id));
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        TenantContext.requirePermission(Permission.CLIENT_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        Client client = new Client();
        client.setTenantId(tenantId);
        applyClientFields(client, request);
        clients.save(client);

        events.publish(new ClientCreated(client.getId(), tenantId));
        audit.publish(new AuditEvent(
                "client.created",
                tenantId,
                TenantContext.requireUserId(),
                "client",
                client.getId().toString(),
                Map.of("name", client.getName()),
                null));
        return ClientResponse.from(client);
    }

    @Transactional
    public ClientResponse update(UUID id, ClientRequest request) {
        TenantContext.requirePermission(Permission.CLIENT_MANAGE);
        Client client = requireClient(id);
        applyClientFields(client, request);
        clients.save(client);

        events.publish(new ClientUpdated(client.getId(), client.getTenantId()));
        audit.publish(new AuditEvent(
                "client.updated",
                client.getTenantId(),
                TenantContext.requireUserId(),
                "client",
                client.getId().toString(),
                Map.of("name", client.getName()),
                null));
        return ClientResponse.from(client);
    }

    @Transactional(readOnly = true)
    public List<ClientContactResponse> listContacts(UUID clientId) {
        TenantContext.requirePermission(Permission.CLIENT_VIEW);
        Client client = requireClient(clientId);
        return contacts.findByClientIdAndTenantIdOrderByNameAsc(client.getId(), client.getTenantId()).stream()
                .map(ClientContactResponse::from)
                .toList();
    }

    @Transactional
    public ClientContactResponse addContact(UUID clientId, ClientContactRequest request) {
        TenantContext.requirePermission(Permission.CLIENT_MANAGE);
        Client client = requireClient(clientId);
        ClientContact contact = new ClientContact();
        contact.setTenantId(client.getTenantId());
        contact.setClientId(client.getId());
        contact.setName(request.name());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setJobTitle(request.jobTitle());
        contact.setPrimaryContact(request.primaryContact());
        contacts.save(contact);
        return ClientContactResponse.from(contact);
    }

    @Transactional(readOnly = true)
    public List<ClientPortalMembershipResponse> listPortalMemberships(UUID clientId) {
        TenantContext.requirePermission(Permission.CLIENT_VIEW);
        Client client = requireClient(clientId);
        return portalMemberships
                .findByClientIdAndTenantIdOrderByCreatedAtAsc(client.getId(), client.getTenantId())
                .stream()
                .map(ClientPortalMembershipResponse::from)
                .toList();
    }

    @Transactional
    public ClientPortalMembershipResponse addPortalMembership(
            UUID clientId, ClientPortalMembershipRequest request) {
        TenantContext.requirePermission(Permission.CLIENT_MANAGE);
        Client client = requireClient(clientId);
        if (request.role() != Role.CLIENT_ADMIN && request.role() != Role.CLIENT_USER) {
            throw new FixnaException(
                    "INVALID_PORTAL_ROLE",
                    HttpStatus.BAD_REQUEST,
                    "Portal membership role must be CLIENT_ADMIN or CLIENT_USER");
        }
        if (users.findById(request.userId()).isEmpty()) {
            throw new FixnaException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found");
        }
        ClientPortalMembership membership = new ClientPortalMembership();
        membership.setTenantId(client.getTenantId());
        membership.setClientId(client.getId());
        membership.setUserId(request.userId());
        membership.setRole(request.role());
        portalMemberships.save(membership);
        return ClientPortalMembershipResponse.from(membership);
    }

    private Client requireClient(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return clients.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Client not found"));
    }

    private static void applyClientFields(Client client, ClientRequest request) {
        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setWebsiteUrl(request.websiteUrl());
        client.setIndustry(request.industry());
        client.setNotes(request.notes());
    }
}
