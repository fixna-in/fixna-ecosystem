package in.fixna.platform.consulting.invoice;

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

import in.fixna.platform.consulting.invoice.dto.InvoiceLineRequest;
import in.fixna.platform.consulting.invoice.dto.InvoiceLineResponse;
import in.fixna.platform.consulting.invoice.dto.InvoiceRequest;
import in.fixna.platform.consulting.invoice.dto.InvoiceResponse;
import in.fixna.platform.consulting.invoice.dto.PaymentRequest;
import in.fixna.platform.consulting.invoice.dto.PaymentResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "invoices", description = "Client invoicing and payment recording")
public class InvoiceController {

    private final InvoiceApplicationService invoiceService;

    public InvoiceController(InvoiceApplicationService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Operation(summary = "List invoices for a client")
    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceResponse>> list(@RequestParam UUID clientId) {
        return ResponseEntity.ok(invoiceService.listByClient(clientId));
    }

    @Operation(summary = "Create a draft invoice")
    @PostMapping("/invoices")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(request));
    }

    @Operation(summary = "Get invoice by id")
    @GetMapping("/invoices/{id}")
    public ResponseEntity<InvoiceResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.get(id));
    }

    @Operation(summary = "Update a draft invoice")
    @PutMapping("/invoices/{id}")
    public ResponseEntity<InvoiceResponse> update(@PathVariable UUID id, @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.update(id, request));
    }

    @Operation(summary = "Add a line item to a draft invoice")
    @PostMapping("/invoices/{id}/lines")
    public ResponseEntity<InvoiceLineResponse> addLine(
            @PathVariable UUID id, @Valid @RequestBody InvoiceLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.addLine(id, request));
    }

    @Operation(summary = "Delete a line item from a draft invoice")
    @DeleteMapping("/invoices/{id}/lines/{lineId}")
    public ResponseEntity<InvoiceResponse> deleteLine(@PathVariable UUID id, @PathVariable UUID lineId) {
        return ResponseEntity.ok(invoiceService.deleteLine(id, lineId));
    }

    @Operation(summary = "Send a draft invoice to the client")
    @PostMapping("/invoices/{id}/send")
    public ResponseEntity<InvoiceResponse> send(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.send(id));
    }

    @Operation(summary = "Cancel an invoice")
    @PostMapping("/invoices/{id}/cancel")
    public ResponseEntity<InvoiceResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.cancel(id));
    }

    @Operation(summary = "List payments for an invoice")
    @GetMapping("/invoices/{id}/payments")
    public ResponseEntity<List<PaymentResponse>> listPayments(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.listPayments(id));
    }

    @Operation(summary = "Record a payment against an invoice")
    @PostMapping("/invoices/{id}/payments")
    public ResponseEntity<PaymentResponse> recordPayment(
            @PathVariable UUID id, @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.recordPayment(id, request));
    }
}
