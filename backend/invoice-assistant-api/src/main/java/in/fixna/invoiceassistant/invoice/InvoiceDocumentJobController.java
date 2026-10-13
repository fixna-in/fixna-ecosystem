package in.fixna.invoiceassistant.invoice;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.invoiceassistant.config.TenantContext;
import in.fixna.invoiceassistant.invoice.dto.InvoiceDocumentJobResponse;
import in.fixna.invoiceassistant.security.Permission;
import in.fixna.platform.common.web.FixnaException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Controller for polling invoice document extraction job status. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "invoice-jobs", description = "Async extraction job polling")
public class InvoiceDocumentJobController {

    private final InvoiceDocumentJobRepository jobRepository;

    public InvoiceDocumentJobController(InvoiceDocumentJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Operation(summary = "Poll extraction job status")
    @GetMapping("/invoice-jobs/{jobId}")
    public ResponseEntity<InvoiceDocumentJobResponse> getJob(@PathVariable UUID jobId) {
        TenantContext.requirePermission(Permission.INVOICE_VIEW);
        UUID tenantId = TenantContext.requireTenantId();
        InvoiceDocumentJob job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "NOT_FOUND", HttpStatus.NOT_FOUND, "Job not found"));
        return ResponseEntity.ok(new InvoiceDocumentJobResponse(
                job.getId(), job.getDocumentId(), job.getStatus(),
                job.getErrorCode(), job.getErrorMessage(), job.getCreatedAt()));
    }
}
