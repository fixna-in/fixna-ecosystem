package in.fixna.platform.consulting.engagement;

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

import in.fixna.platform.consulting.dto.StatusTransitionRequest;
import in.fixna.platform.consulting.engagement.dto.EngagementRequest;
import in.fixna.platform.consulting.engagement.dto.EngagementResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "engagements", description = "Client engagements and lifecycle")
public class EngagementController {

    private final EngagementApplicationService engagementService;

    public EngagementController(EngagementApplicationService engagementService) {
        this.engagementService = engagementService;
    }

    @Operation(summary = "List engagements for a client")
    @GetMapping("/clients/{clientId}/engagements")
    public ResponseEntity<List<EngagementResponse>> listByClient(@PathVariable UUID clientId) {
        return ResponseEntity.ok(engagementService.listByClient(clientId));
    }

    @Operation(summary = "Create an engagement for a client")
    @PostMapping("/clients/{clientId}/engagements")
    public ResponseEntity<EngagementResponse> create(
            @PathVariable UUID clientId, @Valid @RequestBody EngagementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(engagementService.create(clientId, request));
    }

    @Operation(summary = "Get engagement by id")
    @GetMapping("/engagements/{id}")
    public ResponseEntity<EngagementResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(engagementService.get(id));
    }

    @Operation(summary = "Update engagement")
    @PutMapping("/engagements/{id}")
    public ResponseEntity<EngagementResponse> update(
            @PathVariable UUID id, @Valid @RequestBody EngagementRequest request) {
        return ResponseEntity.ok(engagementService.update(id, request));
    }

    @Operation(summary = "Transition engagement status")
    @PostMapping("/engagements/{id}/transition")
    public ResponseEntity<EngagementResponse> transition(
            @PathVariable UUID id, @Valid @RequestBody StatusTransitionRequest request) {
        return ResponseEntity.ok(engagementService.transition(id, request));
    }
}
