package in.fixna.platform.invoiceassist;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.invoiceassist.dto.InvoiceAssistGenerateRequest;
import in.fixna.platform.invoiceassist.dto.InvoiceSuggestionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "invoice-assist", description = "Advisory invoice suggestions (deterministic-first, AI optional)")
public class InvoiceAssistController {

    private final InvoiceAssistApplicationService assistService;

    public InvoiceAssistController(InvoiceAssistApplicationService assistService) {
        this.assistService = assistService;
    }

    @Operation(summary = "List suggestions for an invoice")
    @GetMapping("/invoices/{invoiceId}/assist/suggestions")
    public ResponseEntity<List<InvoiceSuggestionResponse>> list(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(assistService.list(invoiceId));
    }

    @Operation(summary = "Generate suggestions for an invoice (optional AI line ideas)")
    @PostMapping("/invoices/{invoiceId}/assist/suggestions")
    public ResponseEntity<List<InvoiceSuggestionResponse>> generate(
            @PathVariable UUID invoiceId, @RequestBody(required = false) InvoiceAssistGenerateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assistService.generate(invoiceId, request));
    }

    @Operation(summary = "Apply an open due date suggestion to its draft invoice")
    @PostMapping("/assist/suggestions/{suggestionId}/apply")
    public ResponseEntity<InvoiceSuggestionResponse> apply(@PathVariable UUID suggestionId) {
        return ResponseEntity.ok(assistService.apply(suggestionId));
    }

    @Operation(summary = "Dismiss an open suggestion")
    @PostMapping("/assist/suggestions/{suggestionId}/dismiss")
    public ResponseEntity<InvoiceSuggestionResponse> dismiss(@PathVariable UUID suggestionId) {
        return ResponseEntity.ok(assistService.dismiss(suggestionId));
    }
}
