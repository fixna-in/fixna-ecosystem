package in.fixna.platform.websiteaudit;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.ai.AiFeature;
import in.fixna.platform.ai.AiProvider;
import in.fixna.platform.ai.AiUsageLog;
import in.fixna.platform.ai.AiUsageLogRepository;
import in.fixna.platform.ai.dto.AiSuggestionResponse;
import in.fixna.platform.ai.dto.WebsiteAuditFixSuggestion;
import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.profile.ConsultingProfile;
import in.fixna.platform.consulting.profile.ConsultingProfileRepository;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;
import in.fixna.platform.websiteaudit.WebsiteAuditor.AuditOutcome;
import in.fixna.platform.websiteaudit.WebsiteAuditor.CheckResult;
import in.fixna.platform.websiteaudit.WebsiteFetcher.FetchedPage;
import in.fixna.platform.websiteaudit.dto.WebsiteAuditRequest;
import in.fixna.platform.websiteaudit.dto.WebsiteAuditResponse;

@Service
public class WebsiteAuditApplicationService {

    private final WebsiteAuditRepository audits;
    private final WebsiteAuditCheckRepository checks;
    private final WebsiteFetcher fetcher;
    private final WebsiteAuditor auditor;
    private final ConsultingProfileRepository profiles;
    private final AiProvider aiProvider;
    private final AiUsageLogRepository usageLogs;
    private final AuditPublisher audit;

    public WebsiteAuditApplicationService(
            WebsiteAuditRepository audits,
            WebsiteAuditCheckRepository checks,
            WebsiteFetcher fetcher,
            WebsiteAuditor auditor,
            ConsultingProfileRepository profiles,
            AiProvider aiProvider,
            AiUsageLogRepository usageLogs,
            AuditPublisher audit) {
        this.audits = audits;
        this.checks = checks;
        this.fetcher = fetcher;
        this.auditor = auditor;
        this.profiles = profiles;
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
        entity.setNormalizedUrlHash(sha256Hex(normalized(target)));
        entity.setCreatedByUserId(userId);
        entity.setProfileId(matchProfile(tenantId, target));
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

    /** Links the audit to the consulting profile when hosts match. */
    private UUID matchProfile(UUID tenantId, URI target) {
        return profiles.findByTenantId(tenantId)
                .filter(profile -> sameHost(profile.getWebsiteUrl(), target.getHost()))
                .map(ConsultingProfile::getId)
                .orElse(null);
    }

    private static boolean sameHost(String profileUrl, String targetHost) {
        if (profileUrl == null || profileUrl.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(profileUrl.trim());
            return uri.getHost() != null && uri.getHost().equalsIgnoreCase(targetHost);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static String normalized(URI uri) {
        String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
        String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
        return uri.getScheme().toLowerCase(java.util.Locale.ROOT) + "://" + host + path;
    }

    private static String sha256Hex(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
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
