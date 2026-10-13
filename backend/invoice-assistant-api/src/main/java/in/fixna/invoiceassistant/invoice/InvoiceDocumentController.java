package in.fixna.invoiceassistant.invoice;

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

import in.fixna.invoiceassistant.config.TenantContext;
import in.fixna.invoiceassistant.file.FileStorage;
import in.fixna.invoiceassistant.invoice.dto.InvoiceDocumentJobResponse;
import in.fixna.invoiceassistant.invoice.dto.InvoiceDocumentResponse;
import in.fixna.invoiceassistant.invoice.dto.InvoiceExtractionResponse;
import in.fixna.invoiceassistant.invoice.dto.InvoiceExtractionSummary;
import in.fixna.invoiceassistant.invoice.dto.InvoiceFieldConfirmRequest;
import in.fixna.invoiceassistant.invoice.dto.InvoiceFieldResponse;
import in.fixna.invoiceassistant.security.Permission;
import in.fixna.invoiceassistant.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Private invoice document intake (ADR-011). All endpoints require
 * {@code INVOICE_MANAGE} (reads: {@code INVOICE_VIEW}); bytes are only ever
 * streamed back through the authenticated download endpoint — never exposed
 * at a public URL.
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

    @Operation(summary = "Upload a PDF document (optionally linked to an invoice record)")
    @PostMapping(path = "/invoice-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvoiceDocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "invoiceRecordId", required = false) UUID invoiceRecordId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.upload(file, invoiceRecordId));
    }

    @Operation(summary = "List uploaded documents for the tenant")
    @GetMapping("/invoice-documents")
    public ResponseEntity<PageResponse<InvoiceDocumentResponse>> list(
            @RequestParam(required = false) UUID invoiceRecordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(documentService.list(invoiceRecordId, page, size));
    }

    @Operation(summary = "Get one document")
    @GetMapping("/invoice-documents/{documentId}")
    public ResponseEntity<InvoiceDocumentResponse> get(@PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.get(documentId));
    }

    @Operation(summary = "Download a document's original bytes (streamed, tenant-scoped)")
    @GetMapping("/invoice-documents/{documentId}/content")
    public ResponseEntity<byte[]> download(@PathVariable UUID documentId) {
        byte[] bytes = documentService.download(documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + documentService.requireDocument(documentId).getFileName() + "\"")
                .body(bytes);
    }

    @Operation(summary = "Generate a pre-signed download URL for a document")
    @PostMapping("/invoice-documents/{documentId}/download-url")
    public ResponseEntity<FileStorage.PreSignedUrl> generateDownloadUrl(
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "15") int ttlMinutes) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        InvoiceDocument document = documentService.requireDocument(documentId);
        return ResponseEntity.ok(
                fileStorage.generatePresignedUrl(tenantId, document.getStorageKey(), ttlMinutes));
    }

    @Operation(summary = "Hard-delete a document and its extracted data")
    @DeleteMapping("/invoice-documents/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID documentId) {
        documentService.delete(documentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Start async extraction of fields from a document (native text, OCR fallback)")
    @PostMapping("/invoice-documents/{documentId}/extract")
    public ResponseEntity<InvoiceDocumentJobResponse> extract(@PathVariable UUID documentId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(documentService.extract(documentId));
    }

    @Operation(summary = "Get the current extraction with provenance and confidence")
    @GetMapping("/invoice-documents/{documentId}/extraction")
    public ResponseEntity<InvoiceExtractionResponse> getExtraction(@PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.getExtraction(documentId));
    }

    @Operation(summary = "Confirm or edit a field (sets source=MANUAL, records actor)")
    @PostMapping("/invoice-documents/{documentId}/fields/{fieldId}/confirm")
    public ResponseEntity<InvoiceFieldResponse> confirmField(
            @PathVariable UUID documentId,
            @PathVariable UUID fieldId,
            @Valid @RequestBody InvoiceFieldConfirmRequest request) {
        return ResponseEntity.ok(documentService.confirmField(documentId, fieldId, request));
    }

    @Operation(summary = "Approve an extraction (blocked until low-confidence fields are confirmed)")
    @PostMapping("/invoice-documents/{documentId}/approve")
    public ResponseEntity<InvoiceExtractionResponse> approve(@PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.approve(documentId));
    }

    @Operation(summary = "Reject an extraction")
    @PostMapping("/invoice-documents/{documentId}/reject")
    public ResponseEntity<InvoiceExtractionResponse> reject(@PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.reject(documentId));
    }

    @Operation(summary = "Post an approved extraction to an invoice record")
    @PostMapping("/invoice-documents/{documentId}/post")
    public ResponseEntity<InvoiceExtractionResponse> post(@PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.post(documentId));
    }

    @Operation(summary = "Export an extraction as CSV or JSON")
    @GetMapping(path = "/invoice-documents/{documentId}/export")
    public ResponseEntity<String> export(
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "csv") String format) {
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=extraction.json")
                    .body(documentService.exportJson(documentId));
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=extraction.csv")
                .body(documentService.exportCsv(documentId));
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
        return ResponseEntity.ok(documentService.searchExtractions(
                q, status, dateFrom, dateTo, currency, page, size));
    }
}
