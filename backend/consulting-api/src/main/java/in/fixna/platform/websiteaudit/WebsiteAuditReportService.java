package in.fixna.platform.websiteaudit;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;

/**
 * Builds a self-contained, CSP-locked HTML report from a completed audit.
 * All dynamic values are HTML-escaped; no external assets, scripts, or
 * stylesheets — the report is safe to open offline or email as an attachment.
 */
@Service
public class WebsiteAuditReportService {

    private final WebsiteAuditRepository audits;
    private final WebsiteAuditCheckRepository checks;
    private final WebsiteAuditPageRepository pages;

    public WebsiteAuditReportService(
            WebsiteAuditRepository audits,
            WebsiteAuditCheckRepository checks,
            WebsiteAuditPageRepository pages) {
        this.audits = audits;
        this.checks = checks;
        this.pages = pages;
    }

    @Transactional(readOnly = true)
    public String render(UUID auditId) {
        TenantContext.requirePermission(Permission.WEBSITE_AUDIT_VIEW);
        UUID tenantId = TenantContext.requireTenantId();
        WebsiteAudit audit = audits.findByIdAndTenantId(auditId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Website audit not found"));
        List<WebsiteAuditCheck> checkList =
                checks.findByAuditIdAndTenantIdOrderByCheckCodeAsc(auditId, tenantId);
        List<WebsiteAuditPage> pageList =
                pages.findByAuditIdAndTenantIdOrderByDepthAsc(auditId, tenantId);

        StringBuilder html = new StringBuilder(4096);
        html.append("<!doctype html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>Website audit report</title>\n")
                .append("<meta http-equiv=\"Content-Security-Policy\" ")
                .append("content=\"default-src 'none'; style-src 'unsafe-inline'; img-src 'none'\">\n")
                .append("<style>body{font-family:system-ui,sans-serif;max-width:760px;margin:2rem auto;")
                .append("padding:0 1rem;color:#1a2b23}table{border-collapse:collapse;width:100%}")
                .append("td,th{border:1px solid #cdd8d1;padding:.4rem .6rem;text-align:left;font-size:.9rem}")
                .append(".score{font-size:2rem;font-weight:700}.fail{color:#a12622}.pass{color:#2c6b4f}")
                .append("</style>\n</head>\n<body>\n");
        html.append("<h1>Website audit report</h1>\n");
        html.append("<p>URL: <strong>").append(escape(audit.getInputUrl())).append("</strong></p>\n");
        html.append("<p>Status: ").append(escape(audit.getStatus().name()))
                .append(" · Generated: ").append(escape(String.valueOf(audit.getCreatedAt()))).append("</p>\n");
        html.append("<p class=\"score ").append(audit.getOverallScore() >= 70 ? "pass" : "fail")
                .append("\">").append(audit.getOverallScore()).append("/100</p>\n");
        html.append("<p>").append(escape(audit.getSummary())).append("</p>\n");

        html.append("<h2>Checks</h2>\n<table>\n<thead><tr><th>Check</th><th>Severity</th>")
                .append("<th>Score</th><th>Recommendation</th></tr></thead>\n<tbody>\n");
        for (WebsiteAuditCheck check : checkList) {
            html.append("<tr><td>").append(escape(check.getCheckCode())).append("</td><td>")
                    .append(escape(check.getSeverity().name())).append("</td><td>")
                    .append(check.getScore()).append("/100</td><td>")
                    .append(escape(orEmpty(check.getFixHint(), "No action needed.")))
                    .append("</td></tr>\n");
        }
        html.append("</tbody>\n</table>\n");

        if (!pageList.isEmpty()) {
            html.append("<h2>Crawled pages</h2>\n<table>\n<thead><tr><th>URL</th><th>Depth</th>")
                    .append("<th>Status</th><th>Time</th></tr></thead>\n<tbody>\n");
            for (WebsiteAuditPage page : pageList) {
                html.append("<tr><td>").append(escape(page.getUrl())).append("</td><td>")
                        .append(page.getDepth()).append("</td><td>")
                        .append(page.getHttpStatus() == null ? "blocked" : page.getHttpStatus())
                        .append("</td><td>")
                        .append(page.getFetchMs() == null ? "-" : page.getFetchMs() + "ms")
                        .append("</td></tr>\n");
            }
            html.append("</tbody>\n</table>\n");
        }

        html.append("</body>\n</html>\n");
        return html.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private static String orEmpty(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}