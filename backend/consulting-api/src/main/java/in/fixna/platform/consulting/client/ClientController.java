package in.fixna.platform.consulting.client;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.consulting.client.dto.ClientContactRequest;
import in.fixna.platform.consulting.client.dto.ClientContactResponse;
import in.fixna.platform.consulting.client.dto.ClientPortalMembershipRequest;
import in.fixna.platform.consulting.client.dto.ClientPortalMembershipResponse;
import in.fixna.platform.consulting.client.dto.ClientRequest;
import in.fixna.platform.consulting.client.dto.ClientResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/clients")
@Tag(name = "clients", description = "Tenant-scoped client CRM")
public class ClientController {

    private final ClientApplicationService clientService;

    public ClientController(ClientApplicationService clientService) {
        this.clientService = clientService;
    }

    @Operation(summary = "List clients for the current tenant")
    @GetMapping
    public ResponseEntity<List<ClientResponse>> list() {
        return ResponseEntity.ok(clientService.list());
    }

    @Operation(summary = "Create a client")
    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }

    @Operation(summary = "Get client by id")
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(clientService.get(id));
    }

    @Operation(summary = "Update client")
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> update(
            @PathVariable UUID id, @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.ok(clientService.update(id, request));
    }

    @Operation(summary = "List contacts for a client")
    @GetMapping("/{id}/contacts")
    public ResponseEntity<List<ClientContactResponse>> listContacts(@PathVariable UUID id) {
        return ResponseEntity.ok(clientService.listContacts(id));
    }

    @Operation(summary = "Add a contact to a client")
    @PostMapping("/{id}/contacts")
    public ResponseEntity<ClientContactResponse> addContact(
            @PathVariable UUID id, @Valid @RequestBody ClientContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.addContact(id, request));
    }

    @Operation(summary = "List portal memberships for a client")
    @GetMapping("/{id}/portal-memberships")
    public ResponseEntity<List<ClientPortalMembershipResponse>> listPortalMemberships(@PathVariable UUID id) {
        return ResponseEntity.ok(clientService.listPortalMemberships(id));
    }

    @Operation(summary = "Grant portal access for a client user")
    @PostMapping("/{id}/portal-memberships")
    public ResponseEntity<ClientPortalMembershipResponse> addPortalMembership(
            @PathVariable UUID id, @Valid @RequestBody ClientPortalMembershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.addPortalMembership(id, request));
    }
}
