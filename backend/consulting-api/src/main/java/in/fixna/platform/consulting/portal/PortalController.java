package in.fixna.platform.consulting.portal;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.consulting.invoice.dto.InvoiceResponse;
import in.fixna.platform.consulting.meeting.dto.MeetingResponse;
import in.fixna.platform.consulting.portal.dto.PortalInvoiceDetailResponse;
import in.fixna.platform.consulting.portal.dto.PortalMeResponse;
import in.fixna.platform.consulting.project.dto.ProjectResponse;
import in.fixna.platform.consulting.proposal.dto.ProposalResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/portal")
@Tag(name = "portal", description = "Client portal read APIs")
public class PortalController {

    private final PortalApplicationService portalService;

    public PortalController(PortalApplicationService portalService) {
        this.portalService = portalService;
    }

    @Operation(summary = "Current portal user context")
    @GetMapping("/me")
    public ResponseEntity<PortalMeResponse> me() {
        return ResponseEntity.ok(portalService.me());
    }

    @Operation(summary = "List projects for the portal client")
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponse>> listProjects() {
        return ResponseEntity.ok(portalService.listProjects());
    }

    @Operation(summary = "List visible proposals for the portal client")
    @GetMapping("/proposals")
    public ResponseEntity<List<ProposalResponse>> listProposals() {
        return ResponseEntity.ok(portalService.listProposals());
    }

    @Operation(summary = "Get a proposal by id")
    @GetMapping("/proposals/{id}")
    public ResponseEntity<ProposalResponse> getProposal(@PathVariable UUID id) {
        return ResponseEntity.ok(portalService.getProposal(id));
    }

    @Operation(summary = "Approve a sent proposal (CLIENT_ADMIN)")
    @PostMapping("/proposals/{id}/approve")
    public ResponseEntity<ProposalResponse> approveProposal(@PathVariable UUID id) {
        return ResponseEntity.ok(portalService.approveProposal(id));
    }

    @Operation(summary = "Reject a sent proposal (CLIENT_ADMIN)")
    @PostMapping("/proposals/{id}/reject")
    public ResponseEntity<ProposalResponse> rejectProposal(@PathVariable UUID id) {
        return ResponseEntity.ok(portalService.rejectProposal(id));
    }

    @Operation(summary = "List visible invoices for the portal client")
    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceResponse>> listInvoices() {
        return ResponseEntity.ok(portalService.listInvoices());
    }

    @Operation(summary = "Get invoice detail with lines and payments")
    @GetMapping("/invoices/{id}")
    public ResponseEntity<PortalInvoiceDetailResponse> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(portalService.getInvoice(id));
    }

    @Operation(summary = "List meetings for the portal client")
    @GetMapping("/meetings")
    public ResponseEntity<List<MeetingResponse>> listMeetings() {
        return ResponseEntity.ok(portalService.listMeetings());
    }
}
