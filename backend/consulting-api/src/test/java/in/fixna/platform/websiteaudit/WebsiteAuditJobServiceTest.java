package in.fixna.platform.websiteaudit;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.profile.ConsultingProfileRepository;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.TenantContext;
import in.fixna.platform.websiteaudit.dto.WebsiteAuditRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock ConsultingProfileRepository profiles;
    @Mock AuditPublisher audit;

    @InjectMocks WebsiteAuditJobService service;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void submitRejectsSecondRunningCrawlWithConflict() {
        TenantContext.set(tenantId, userId, Role.CONSULTANT);
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
}
