package in.fixna.websiteaudit.job;

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

import in.fixna.websiteaudit.WebsiteAudit;
import in.fixna.websiteaudit.WebsiteAuditJob;
import in.fixna.websiteaudit.WebsiteAuditJobRepository;
import in.fixna.websiteaudit.WebsiteAuditJobStatus;
import in.fixna.websiteaudit.WebsiteAuditPageRepository;
import in.fixna.websiteaudit.WebsiteAuditRepository;
import in.fixna.websiteaudit.WebsiteAuditStatus;
import in.fixna.websiteaudit.audit.AuditEvent;
import in.fixna.websiteaudit.audit.AuditPublisher;
import in.fixna.websiteaudit.config.TenantContext;
import in.fixna.websiteaudit.dto.WebsiteAuditJobResponse;
import in.fixna.websiteaudit.dto.WebsiteAuditRequest;
import in.fixna.platform.common.web.FixnaException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards for async job submission (ADR-009): one RUNNING crawl per tenant
 * surfaces as 409 CRAWL_IN_PROGRESS instead of a DB constraint failure.
 */
@ExtendWith(MockitoExtension.class)
class WebsiteAuditJobServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Mock WebsiteAuditJobRepository jobs;
    @Mock WebsiteAuditRepository audits;
    @Mock WebsiteAuditPageRepository pages;
    @Mock WebsiteAuditPersistence persistence;
    @Mock AuditPublisher audit;

    @InjectMocks WebsiteAuditJobService service;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void submitRejectsSecondRunningCrawlWithConflict() {
        TenantContext.set(tenantId, userId);
        WebsiteAuditJob running = new WebsiteAuditJob();
        running.setStatus(WebsiteAuditJobStatus.RUNNING);
        when(jobs.findFirstByTenantIdAndStatus(tenantId, WebsiteAuditJobStatus.RUNNING))
                .thenReturn(Optional.of(running));

        assertThatThrownBy(() -> service.submit(new WebsiteAuditRequest("https://example.com/")))
                .isInstanceOf(FixnaException.class)
                .satisfies(ex -> {
                    FixnaException failed = (FixnaException) ex;
                    assertThat(failed.getCode()).isEqualTo("CRAWL_IN_PROGRESS");
                    assertThat(failed.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void submitQueuesJobAndCreatesRunningAudit() {
        TenantContext.set(tenantId, userId);

        WebsiteAuditJobResponse response =
                service.submit(new WebsiteAuditRequest("https://example.com/"));

        assertThat(response.jobId()).isNotNull();
        assertThat(response.status()).isEqualTo("QUEUED");
        assertThat(response.auditId()).isNotNull();
        assertThat(response.maxAttempts()).isEqualTo(WebsiteAuditJobService.MAX_ATTEMPTS);
        assertThat(response.progress().budgetPages()).isEqualTo(WebsiteAuditJobService.BUDGET_PAGES);
        assertThat(response.progress().budgetSeconds()).isEqualTo(WebsiteAuditJobService.BUDGET_SECONDS);
        assertThat(response.linksSelf()).isEqualTo("/api/v1/website-audits/jobs/" + response.jobId());
        assertThat(response.linksAudit()).isEqualTo("/api/v1/website-audits/" + response.auditId());

        ArgumentCaptor<WebsiteAudit> auditCaptor = ArgumentCaptor.forClass(WebsiteAudit.class);
        verify(audits).save(auditCaptor.capture());
        WebsiteAudit savedAudit = auditCaptor.getValue();
        assertThat(savedAudit.getTenantId()).isEqualTo(tenantId);
        assertThat(savedAudit.getCreatedByUserId()).isEqualTo(userId);
        assertThat(savedAudit.getStatus()).isEqualTo(WebsiteAuditStatus.RUNNING);
        assertThat(savedAudit.getInputUrl()).isEqualTo("https://example.com/");

        verify(audit).publish(any(AuditEvent.class));
    }

    @Test
    void submitRejectsNonHttpUrl() {
        TenantContext.set(tenantId, userId);

        assertThatThrownBy(() -> service.submit(new WebsiteAuditRequest("ftp://example.com/")))
                .isInstanceOf(FixnaException.class)
                .satisfies(ex -> {
                    FixnaException failed = (FixnaException) ex;
                    assertThat(failed.getCode()).isEqualTo("INVALID_URL");
                    assertThat(failed.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }
}
