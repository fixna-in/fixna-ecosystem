package in.fixna.platform.websiteaudit;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.TenantContext;
import in.fixna.platform.websiteaudit.WebsiteAuditor.AuditOutcome;
import in.fixna.platform.websiteaudit.WebsiteAuditor.CheckResult;
import in.fixna.platform.websiteaudit.WebsiteFetcher.FetchedPage;
import in.fixna.platform.websiteaudit.dto.WebsiteAuditRequest;
import in.fixna.platform.websiteaudit.dto.WebsiteAuditResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebsiteAuditApplicationServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID auditId = UUID.randomUUID();

    @Mock WebsiteAuditRepository audits;
    @Mock WebsiteAuditCheckRepository checks;
    @Mock WebsiteFetcher fetcher;
    @Mock WebsiteAuditor auditor;
    @Mock ConsultingProfileRepository profiles;
    @Mock AiProvider aiProvider;
    @Mock AiUsageLogRepository usageLogs;
    @Mock AuditPublisher audit;

    @InjectMocks WebsiteAuditApplicationService service;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private void asConsultant() {
        TenantContext.set(tenantId, userId, Role.CONSULTANT);
    }

    private static FetchedPage page(String uri, int status, String contentType, String body, long ms) {
        URI parsed = URI.create(uri);
        return new FetchedPage(parsed, parsed, status, ms, contentType, body);
    }

    private static AuditOutcome outcome(int score, String summary) {
        return new AuditOutcome(
                List.of(new CheckResult("title", AuditSeverity.WARNING, score, Map.of(), null)),
                score,
                summary);
    }

    private void stubSuccessfulFetch() {
        when(fetcher.fetch(any(URI.class))).thenReturn(
                Optional.of(page("https://example.com/", 200, "text/html", "<html></html>", 120)),
                Optional.of(page("https://example.com/robots.txt", 200, "text/plain", "", 30)),
                Optional.of(page("https://example.com/sitemap.xml", 200, "text/xml", "", 30)));
    }

    @Test
    void runPersistsAuditAndChecksAndPublishesAuditEvent() {
        asConsultant();
        stubSuccessfulFetch();
        when(auditor.audit(any(), any(), any()))
                .thenReturn(outcome(100, "1 of 1 checks passed. Overall score 100/100."));
        when(audits.save(any(WebsiteAudit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checks.save(any(WebsiteAuditCheck.class))).thenAnswer(inv -> inv.getArgument(0));

        WebsiteAuditResponse response = service.run(new WebsiteAuditRequest("https://example.com/"));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.overallScore()).isEqualTo(100);
        verify(checks, times(1)).save(any(WebsiteAuditCheck.class));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).publish(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("website.audit.completed");
        assertThat(auditCaptor.getValue().details()).containsEntry("score", "100");
    }

    @Test
    void runLinksAuditToProfileWhenHostMatches() {
        asConsultant();
        ConsultingProfile profile = new ConsultingProfile();
        profile.setId(UUID.randomUUID());
        profile.setTenantId(tenantId);
        profile.setWebsiteUrl("https://example.com/about");
        when(profiles.findByTenantId(tenantId)).thenReturn(Optional.of(profile));
        stubSuccessfulFetch();
        when(auditor.audit(any(), any(), any())).thenReturn(outcome(80, "summary"));
        when(audits.save(any(WebsiteAudit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checks.save(any(WebsiteAuditCheck.class))).thenAnswer(inv -> inv.getArgument(0));

        WebsiteAuditResponse response = service.run(new WebsiteAuditRequest("https://example.com/"));

        assertThat(response.profileId()).isEqualTo(profile.getId());
    }

    @Test
    void clientUserCannotRunAudit() {
        TenantContext.set(tenantId, userId, Role.CLIENT_USER);

        assertThatThrownBy(() -> service.run(new WebsiteAuditRequest("https://example.com/")))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(fetcher, never()).fetch(any(URI.class));
    }

    @Test
    void invalidUrlIsRejected() {
        asConsultant();

        assertThatThrownBy(() -> service.run(new WebsiteAuditRequest("not a url")))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_URL");
        assertThatThrownBy(() -> service.run(new WebsiteAuditRequest("ftp://example.com/")))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_URL");
    }

    @Test
    void unreachableTargetMarksAuditFailed() {
        asConsultant();
        when(fetcher.fetch(any(URI.class))).thenReturn(Optional.empty());
        when(audits.save(any(WebsiteAudit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checks.save(any(WebsiteAuditCheck.class))).thenAnswer(inv -> inv.getArgument(0));

        WebsiteAuditResponse response = service.run(new WebsiteAuditRequest("https://down.example.com/"));

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.overallScore()).isZero();
        assertThat(response.summary()).contains("could not complete");
        verify(auditor, never()).audit(any(), any(), any());

        ArgumentCaptor<WebsiteAuditCheck> checkCaptor = ArgumentCaptor.forClass(WebsiteAuditCheck.class);
        verify(checks).save(checkCaptor.capture());
        assertThat(checkCaptor.getValue().getCheckCode()).isEqualTo("fetch_failed");
    }

    @Test
    void listReturnsTenantAudits() {
        asConsultant();
        WebsiteAudit entity = new WebsiteAudit();
        entity.setId(auditId);
        entity.setTenantId(tenantId);
        entity.setInputUrl("https://example.com/");
        entity.setStatus(WebsiteAuditStatus.COMPLETED);
        when(audits.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(entity));

        List<WebsiteAuditResponse> response = service.list();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().inputUrl()).isEqualTo("https://example.com/");
    }

    @Test
    void crossTenantGetReturnsNotFound() {
        asConsultant();
        when(audits.findByIdAndTenantId(auditId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(auditId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void suggestFixesReturnsAdvisoryAiSuggestions() {
        asConsultant();
        WebsiteAudit entity = new WebsiteAudit();
        entity.setId(auditId);
        entity.setTenantId(tenantId);
        entity.setInputUrl("https://example.com/");
        entity.setStatus(WebsiteAuditStatus.COMPLETED);
        when(audits.findByIdAndTenantId(auditId, tenantId)).thenReturn(Optional.of(entity));

        WebsiteAuditCheck failing = new WebsiteAuditCheck();
        failing.setCheckCode("title");
        failing.setScore(40);
        failing.setFixHint("Add a title");
        when(checks.findByAuditIdAndTenantIdOrderByCheckCodeAsc(auditId, tenantId)).thenReturn(List.of(failing));
        when(aiProvider.suggestWebsiteAuditFixes("https://example.com/", List.of("title: Add a title")))
                .thenReturn(new WebsiteAuditFixSuggestion("https://example.com/", List.of("Fix the title")));
        when(usageLogs.save(any(AiUsageLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AiSuggestionResponse<WebsiteAuditFixSuggestion> response = service.suggestFixes(auditId);

        assertThat(response.disclaimer()).isEqualTo(AiSuggestionResponse.ADVISORY_DISCLAIMER);
        assertThat(response.suggestion().priorities()).containsExactly("Fix the title");

        ArgumentCaptor<AiUsageLog> logCaptor = ArgumentCaptor.forClass(AiUsageLog.class);
        verify(usageLogs).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getFeature()).isEqualTo(AiFeature.WEBSITE_AUDIT_FIXES);
        assertThat(logCaptor.getValue().getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void suggestFixesRequiresPermissionAndAudit() {
        TenantContext.set(tenantId, userId, Role.CLIENT_USER);

        assertThatThrownBy(() -> service.suggestFixes(auditId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(usageLogs, never()).save(any(AiUsageLog.class));
    }
}
