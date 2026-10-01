package in.fixna.platform.consulting.proposal;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.consulting.proposal.dto.ProposalItemRequest;
import in.fixna.platform.consulting.proposal.dto.ProposalItemResponse;
import in.fixna.platform.consulting.proposal.dto.ProposalRequest;
import in.fixna.platform.consulting.proposal.dto.ProposalResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "proposals", description = "Client proposals and approval lifecycle")
public class ProposalController {

    private final ProposalApplicationService proposalService;

    public ProposalController(ProposalApplicationService proposalService) {
        this.proposalService = proposalService;
    }

    @Operation(summary = "List proposals for a client")
    @GetMapping("/proposals")
    public ResponseEntity<List<ProposalResponse>> list(@RequestParam UUID clientId) {
        return ResponseEntity.ok(proposalService.listByClient(clientId));
    }

    @Operation(summary = "Create a draft proposal")
    @PostMapping("/proposals")
    public ResponseEntity<ProposalResponse> create(@Valid @RequestBody ProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proposalService.create(request));
    }

    @Operation(summary = "Get proposal by id")
    @GetMapping("/proposals/{id}")
    public ResponseEntity<ProposalResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(proposalService.get(id));
    }

    @Operation(summary = "Update a draft proposal")
    @PutMapping("/proposals/{id}")
    public ResponseEntity<ProposalResponse> update(
            @PathVariable UUID id, @Valid @RequestBody ProposalRequest request) {
        return ResponseEntity.ok(proposalService.update(id, request));
    }

    @Operation(summary = "Add a line item to a draft proposal")
    @PostMapping("/proposals/{id}/items")
    public ResponseEntity<ProposalItemResponse> addItem(
            @PathVariable UUID id, @Valid @RequestBody ProposalItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proposalService.addItem(id, request));
    }

    @Operation(summary = "Update a line item on a draft proposal")
    @PutMapping("/proposals/{id}/items/{itemId}")
    public ResponseEntity<ProposalItemResponse> updateItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody ProposalItemRequest request) {
        return ResponseEntity.ok(proposalService.updateItem(id, itemId, request));
    }

    @Operation(summary = "Delete a line item from a draft proposal")
    @DeleteMapping("/proposals/{id}/items/{itemId}")
    public ResponseEntity<ProposalResponse> deleteItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        return ResponseEntity.ok(proposalService.deleteItem(id, itemId));
    }

    @Operation(summary = "Send a draft proposal to the client")
    @PostMapping("/proposals/{id}/send")
    public ResponseEntity<ProposalResponse> send(@PathVariable UUID id) {
        return ResponseEntity.ok(proposalService.send(id));
    }

    @Operation(summary = "Approve a sent proposal")
    @PostMapping("/proposals/{id}/approve")
    public ResponseEntity<ProposalResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(proposalService.approve(id));
    }

    @Operation(summary = "Reject a sent proposal")
    @PostMapping("/proposals/{id}/reject")
    public ResponseEntity<ProposalResponse> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(proposalService.reject(id));
    }

    @Operation(summary = "Cancel a draft or sent proposal")
    @PostMapping("/proposals/{id}/cancel")
    public ResponseEntity<ProposalResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(proposalService.cancel(id));
    }
}
