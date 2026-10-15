package in.fixna.websiteaudit.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.websiteaudit.ai.dto.AiSuggestionResponse;
import in.fixna.websiteaudit.ai.dto.WebsiteAuditFixSuggestion;
import in.fixna.websiteaudit.dto.WebsiteAuditJobResponse;
import in.fixna.websiteaudit.dto.WebsiteAuditRequest;
import in.fixna.websiteaudit.dto.WebsiteAuditResponse;
import in.fixna.websiteaudit.job.WebsiteAuditApplicationService;
import in.fixna.websiteaudit.job.WebsiteAuditJobService;
import in.fixna.websiteaudit.report.WebsiteAuditReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "website-audits", description = "Deterministic website audits with advisory AI fixes")
public class WebsiteAuditController {

    private final WebsiteAuditApplicationService auditService;
    private final WebsiteAuditJobService jobService;
    private final WebsiteAuditReportService reportService;

    public WebsiteAuditController(
            WebsiteAuditApplicationService auditService,
            WebsiteAuditJobService jobService,
            WebsiteAuditReportService reportService) {
        this.auditService = auditService;
        this.jobService = jobService;
        this.reportService = reportService;
    }

    @Operation(summary = "Run a website audit for a URL")
    @PostMapping("/website-audits")
    public ResponseEntity<WebsiteAuditResponse> run(@Valid @RequestBody WebsiteAuditRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auditService.run(request));
    }

    @Operation(summary = "Submit a bounded website audit job (202 async model, ADR-009)")
    @PostMapping("/website-audits/jobs")
    public ResponseEntity<WebsiteAuditJobResponse> submit(@Valid @RequestBody WebsiteAuditRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobService.submit(request));
    }

    @Operation(summary = "List website audit jobs for the tenant")
    @GetMapping("/website-audits/jobs")
    public ResponseEntity<List<WebsiteAuditJobResponse>> listJobs() {
        return ResponseEntity.ok(jobService.list());
    }

    @Operation(summary = "Poll a website audit job")
    @GetMapping("/website-audits/jobs/{id}")
    public ResponseEntity<WebsiteAuditJobResponse> poll(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.poll(id));
    }

    @Operation(summary = "Cancel a queued or running website audit job")
    @PostMapping("/website-audits/jobs/{id}/cancel")
    public ResponseEntity<WebsiteAuditJobResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.cancel(id));
    }

    @Operation(summary = "List website audits for the tenant")
    @GetMapping("/website-audits")
    public ResponseEntity<List<WebsiteAuditResponse>> list() {
        return ResponseEntity.ok(auditService.list());
    }

    @Operation(summary = "Get a website audit with its checks")
    @GetMapping("/website-audits/{id}")
    public ResponseEntity<WebsiteAuditResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.get(id));
    }

    @Operation(summary = "Download a self-contained HTML report for a completed audit")
    @GetMapping("/website-audits/{id}/report")
    public ResponseEntity<String> report(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'")
                .header("X-Content-Type-Options", "nosniff")
                .body(reportService.render(id));
    }

    @Operation(summary = "Get advisory AI fixes for failing audit checks")
    @PostMapping("/website-audits/{id}/fixes")
    public ResponseEntity<AiSuggestionResponse<WebsiteAuditFixSuggestion>> suggestFixes(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.suggestFixes(id));
    }
}
