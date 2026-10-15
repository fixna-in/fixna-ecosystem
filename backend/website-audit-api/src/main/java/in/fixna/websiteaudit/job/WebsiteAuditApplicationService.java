package in.fixna.websiteaudit.job;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.websiteaudit.WebsiteAudit;
import in.fixna.websiteaudit.WebsiteAuditCheck;
import in.fixna.websiteaudit.WebsiteAuditCheckRepository;
import in.fixna.websiteaudit.WebsiteAuditRepository;
import in.fixna.websiteaudit.WebsiteAuditStatus;
import in.fixna.websiteaudit.ai.AiFeature;
import in.fixna.websiteaudit.ai.AiProvider;
import in.fixna.websiteaudit.ai.AiUsageLog;
import in.fixna.websiteaudit.ai.AiUsageLogRepository;
import in.fixna.websiteaudit.ai.dto.AiSuggestionResponse;
import in.fixna.websiteaudit.ai.dto.WebsiteAuditFixSuggestion;
import in.fixna.websiteaudit.audit.AuditEvent;
import in.fixna.websiteaudit.audit.AuditPublisher;
import in.fixna.websiteaudit.check.WebsiteAuditor;
import in.fixna.websiteaudit.check.WebsiteAuditor.AuditOutcome;
import in.fixna.websiteaudit.check.WebsiteAuditor.CheckResult;
import in.fixna.websiteaudit.check.WebsiteCrawler;
import in.fixna.websiteaudit.check.WebsiteCrawler.CrawlBounds;
import in.fixna.websiteaudit.check.WebsiteCrawler.CrawlResult;
import in.fixna.websiteaudit.config.TenantContext;
import in.fixna.websiteaudit.dto.WebsiteAuditRequest;
import in.fixna.websiteaudit.dto.WebsiteAuditResponse;
import in.fixna.websiteaudit.fetcher.WebsiteFetcher;
import in.fixna.websiteaudit.fetcher.WebsiteFetcher.FetchedPage;
import in.fixna.websiteaudit.security.Permission;
import in.fixna.platform.common.web.FixnaException;

/**
 * Sync audit (MVP) and AI advisory fixes. Core checks are deterministic;
 * AI only explains/prioritizes. AI unavailability never blocks the report.
 */
@Service
public class WebsiteAuditApplicationService {

    private final WebsiteAuditRepository audits;
    private final WebsiteAuditCheckRepository checks;
    private final WebsiteFetcher fetcher;
    private final WebsiteAuditor auditor;
    private final WebsiteCrawler crawler;
    private final AiProvider aiProvider;
    private final AiUsageLogRepository usageLogs;
    private final AuditPublisher audit;

    public WebsiteAuditApplicationService(
            WebsiteAuditRepository audits,
            WebsiteAuditCheckRepository checks,
            WebsiteFetcher fetcher,
            WebsiteAuditor auditor,
            WebsiteCrawler crawler,
            AiProvider aiProvider,
            AiUsageLogRepository usageLogs,
            AuditPublisher audit) {
        this.audits = audits;
        this.checks = checks;
        this.fetcher = fetcher;
        this.auditor = auditor;
        this.crawler = crawler;
        this.aiProvider = aiProvider;
        this.usageLogs = usageLogs;
        this.audit = audit;
    }

    @Transactional
    public WebsiteAuditResponse run(WebsiteAuditRequest request) {
        TenantContext.requirePermission(Permission.WEBSITE_AUDIT_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        URI target = parseTarget(request.url());

        Optional<FetchedPage> page = fetcher.fetch(target);
        Optional<FetchedPage> robots = page.isPresent()
                ? fetcher.fetch(WebsiteFetcher.sibling(target, "/robots.txt"))
                : Optional.empty();
        Optional<FetchedPage> sitemap = page.isPresent()
                ? fetcher.fetch(WebsiteFetcher.sibling(target, "/sitemap.xml"))
                : Optional.empty();

        AuditOutcome outcome = page
                .map(p -> auditor.audit(p, robots.orElse(null), sitemap.orElse(null)))
                .orElseGet(() -> WebsiteAuditor.failed("target unreachable or blocked"));

        WebsiteAudit entity = new WebsiteAudit();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setInputUrl(request.url().trim());
        entity.setNormalizedUrlHash(WebsiteAuditPersistence.sha256Hex(WebsiteAuditPersistence.normalized(target)));
        entity.setCreatedByUserId(userId);
        entity.setStatus(page.isPresent() ? WebsiteAuditStatus.COMPLETED : WebsiteAuditStatus.FAILED);
        entity.setSummary(outcome.summary());
        entity.setOverallScore(outcome.overallScore());
        audits.save(entity);

        for (CheckResult result : outcome.checks()) {
            WebsiteAuditCheck check = new WebsiteAuditCheck();
            check.setTenantId(tenantId);
            check.setAuditId(entity.getId());
            check.setCheckCode(result.code());
            check.setSeverity(result.severity());
            check.setScore(result.score());
            check.setEvidence(result.evidence());
            check.setFixHint(result.fixHint());
            checks.save(check);
        }

        audit.publish(new AuditEvent(
                "website.audit.completed",
                tenantId,
                userId,
                "website_audit",
                entity.getId().toString(),
                Map.of("inputUrl", entity.getInputUrl(), "score", String.valueOf(entity.getOverallScore())),
                null));
        return WebsiteAuditResponse.from(entity, List.of());
    }

    @Transactional(readOnly = true)
    public List<WebsiteAuditResponse> list() {
        TenantContext.requirePermission(Permission.WEBSITE_AUDIT_VIEW);
        UUID tenantId = TenantContext.requireTenantId();
        return audits.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(found -> WebsiteAuditResponse.from(found, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public WebsiteAuditResponse get(UUID id) {
        TenantContext.requirePermission(Permission.WEBSITE_AUDIT_VIEW);
        WebsiteAudit audit = requireAudit(id);
        UUID tenantId = TenantContext.requireTenantId();
        return WebsiteAuditResponse.from(
                audit, checks.findByAuditIdAndTenantIdOrderByCheckCodeAsc(audit.getId(), tenantId));
    }

    @Transactional
    public AiSuggestionResponse<WebsiteAuditFixSuggestion> suggestFixes(UUID id) {
        TenantContext.requirePermission(Permission.WEBSITE_AUDIT_MANAGE);
        TenantContext.requirePermission(Permission.AI_USE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        WebsiteAudit audit = requireAudit(id);

        List<String> findings = checks.findByAuditIdAndTenantIdOrderByCheckCodeAsc(audit.getId(), tenantId).stream()
                .filter(check -> check.getScore() < 100)
                .map(check -> check.getCheckCode() + ": " + orEmpty(check.getFixHint()))
                .toList();

        WebsiteAuditFixSuggestion suggestion = aiProvider.suggestWebsiteAuditFixes(audit.getInputUrl(), findings);
        logUsage(tenantId, userId, AiFeature.WEBSITE_AUDIT_FIXES, "auditId=" + id);
        publishAiAudit(tenantId, userId, AiFeature.WEBSITE_AUDIT_FIXES, id.toString());
        return AiSuggestionResponse.of(suggestion);
    }

    private WebsiteAudit requireAudit(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return audits.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Website audit not found"));
    }

    private static URI parseTarget(String raw) {
        try {
            URI uri = new URI(raw.trim());
            String scheme = uri.getScheme();
            boolean http = scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
            if (!http || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new FixnaException("INVALID_URL", HttpStatus.BAD_REQUEST, "URL must be a valid http(s) URL");
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new FixnaException("INVALID_URL", HttpStatus.BAD_REQUEST, "URL must be a valid http(s) URL");
        }
    }

    private static String orEmpty(String value) {
        return value == null || value.isBlank() ? "review recommended" : value;
    }

    private void logUsage(UUID tenantId, UUID userId, AiFeature feature, String requestSummary) {
        AiUsageLog log = new AiUsageLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setFeature(feature);
        log.setRequestSummary(requestSummary);
        usageLogs.save(log);
    }

    private void publishAiAudit(UUID tenantId, UUID userId, AiFeature feature, String entityId) {
        audit.publish(new AuditEvent(
                "ai.suggestion.generated",
                tenantId,
                userId,
                "ai_suggestion",
                entityId,
                Map.of("feature", feature.name()),
                null));
    }
}