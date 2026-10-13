package in.fixna.platform.invoiceassist;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.fixna.platform.ai.dto.AiSuggestionResponse;
import in.fixna.platform.ai.dto.InvoiceLineIdeasSuggestion;
import in.fixna.platform.common.web.PageResponse;
import in.fixna.platform.file.FileStorage;
import in.fixna.platform.invoiceassist.dto.InvoiceDocumentJobResponse;
import in.fixna.platform.invoiceassist.dto.InvoiceDocumentResponse;
import in.fixna.platform.invoiceassist.dto.InvoiceExtractionResponse;
import in.fixna.platform.invoiceassist.dto.InvoiceExtractionSummary;
import in.fixna.platform.invoiceassist.dto.InvoiceFieldConfirmRequest;
import in.fixna.platform.invoiceassist.dto.InvoiceFieldResponse;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Private invoice document intake (ADR-011). All endpoints require
 * {@code INVOICE_MANAGE}; bytes are only ever streamed back through the
 * authenticated download endpoint — never exposed at a public URL.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "invoice-documents", description = "Private PDF intake with provenance-backed extraction")
public class InvoiceDocumentController {

    private final InvoiceDocumentService documentService;
    private final FileStorage fileStorage;

    public InvoiceDocumentController(InvoiceDocumentService documentService, FileStorage fileStorage) {
        this.documentService = documentService;
        this.fileStorage = fileStorage;
    }

    @Operation(summary = "Upload a PDF document for an invoice")
    @PostMapping(path = "/invoices/{invoiceId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvoiceDocumentResponse> upload(
            @PathVariable UUID invoiceId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.upload(invoiceId, file));
    }

    @Operation(summary = "List documents for an invoice")
    @GetMapping("/invoices/{invoiceId}/documents")
    public ResponseEntity<List<InvoiceDocumentResponse>> list(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(documentService.list(invoiceId));
    }

    @Operation(summary = "Download a document's original bytes (streamed, tenant-scoped)")
    @GetMapping("/invoices/{invoiceId}/documents/{documentId}/content")
    public ResponseEntity<byte[]> download(@PathVariable UUID invoiceId, @PathVariable UUID documentId) {
        byte[] bytes = documentService.download(invoiceId, documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .header("X-Content-Type-Options", "nosniff")
                .body(bytes);
    }

    @Operation(summary = "Generate a pre-signed download URL for a document")
    @PostMapping("/invoices/{invoiceId}/documents/{documentId}/download-url")
    public ResponseEntity<FileStorage.PreSignedUrl> generateDownloadUrl(
            @PathVariable UUID invoiceId, @PathVariable UUID documentId,
            @RequestParam(defaultValue = "15") int ttlMinutes) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        InvoiceDocument document = documentService.requireDocument(invoiceId, documentId);
        FileStorage.PreSignedUrl preSignedUrl = fileStorage.generatePresignedUrl(tenantId, document.getStorageKey(), ttlMinutes);
        return ResponseEntity.ok(preSignedUrl);
    }

    @Operation(summary = "Hard-delete a document and its extracted data")
    @DeleteMapping("/invoices/{invoiceId}/documents/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID invoiceId, @PathVariable UUID documentId) {
        documentService.delete(invoiceId, documentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Start async extraction of fields from a document (native text, OCR fallback)")
    @PostMapping("/invoices/{invoiceId}/documents/{documentId}/extract")
    public ResponseEntity<InvoiceDocumentJobResponse> extract(
            @PathVariable UUID invoiceId, @PathVariable UUID documentId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(documentService.extract(invoiceId, documentId));
    }

    @Operation(summary = "Get the current extraction with provenance and confidence")
    @GetMapping("/invoices/{invoiceId}/documents/{documentId}/extraction")
    public ResponseEntity<InvoiceExtractionResponse> getExtraction(
            @PathVariable UUID invoiceId, @PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.getExtraction(invoiceId, documentId));
    }

    @Operation(summary = "Confirm or edit a field (sets source=MANUAL, records actor)")
    @PostMapping("/invoices/{invoiceId}/documents/{documentId}/fields/{fieldId}/confirm")
    public ResponseEntity<InvoiceFieldResponse> confirmField(
            @PathVariable UUID invoiceId,
            @PathVariable UUID documentId,
            @PathVariable UUID fieldId,
            @Valid @RequestBody InvoiceFieldConfirmRequest request) {
        return ResponseEntity.ok(documentService.confirmField(invoiceId, documentId, fieldId, request));
    }

    @Operation(summary = "Approve an extraction (blocked until low-confidence fields are confirmed)")
    @PostMapping("/invoices/{invoiceId}/documents/{documentId}/approve")
    public ResponseEntity<InvoiceExtractionResponse> approve(
            @PathVariable UUID invoiceId, @PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.approve(invoiceId, documentId));
    }

    @Operation(summary = "Reject an extraction")
    @PostMapping("/invoices/{invoiceId}/documents/{documentId}/reject")
    public ResponseEntity<InvoiceExtractionResponse> reject(
            @PathVariable UUID invoiceId, @PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.reject(invoiceId, documentId));
    }

    @Operation(summary = "Post an approved extraction to the draft invoice")
    @PostMapping("/invoices/{invoiceId}/documents/{documentId}/post")
    public ResponseEntity<InvoiceExtractionResponse> post(
            @PathVariable UUID invoiceId, @PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.post(invoiceId, documentId));
    }

    @Operation(summary = "Export an extraction as CSV or JSON")
    @GetMapping(path = "/invoices/{invoiceId}/documents/{documentId}/export")
    public ResponseEntity<String> export(
            @PathVariable UUID invoiceId,
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "csv") String format) {
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=extraction.json")
                    .body(documentService.exportJson(invoiceId, documentId));
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=extraction.csv")
                .body(documentService.exportCsv(invoiceId, documentId));
    }

    @Operation(summary = "Advisory AI line-item ideas for an invoice")
    @PostMapping("/invoices/{invoiceId}/documents/line-ideas")
    public ResponseEntity<AiSuggestionResponse<InvoiceLineIdeasSuggestion>> suggestLineIdeas(
            @PathVariable UUID invoiceId) {
        return ResponseEntity.ok(documentService.suggestLineIdeas(invoiceId));
    }

    @Operation(summary = "Search extractions across documents")
    @GetMapping("/invoice-extractions/search")
    public ResponseEntity<PageResponse<InvoiceExtractionSummary>> searchExtractions(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) InvoiceExtractionStatus status,
            @RequestParam(required = false) java.time.OffsetDateTime dateFrom,
            @RequestParam(required = false) java.time.OffsetDateTime dateTo,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(documentService.searchExtractions(q, status, dateFrom, dateTo, currency, page, size));
    }
}