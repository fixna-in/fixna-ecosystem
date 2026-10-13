package in.fixna.invoiceassistant.invoice;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resets monthly quotas for tenants when their reset period has passed.
 * Runs daily to check for quotas that need reset.
 */
@Component
public class TenantQuotaResetJob {

    private static final Logger LOG = LoggerFactory.getLogger("fixna.invoice.quota");

    private final TenantQuotaRepository quotaRepository;

    public TenantQuotaResetJob(TenantQuotaRepository quotaRepository) {
        this.quotaRepository = quotaRepository;
    }

    /**
     * Runs every day at 00:30 to check for quotas that need reset.
     * The cron expression can be overridden via fixna.invoice.quota-reset-cron.
     */
    @Scheduled(cron = "${fixna.invoice.quota-reset-cron:0 30 0 * * *}")
    @Transactional
    public void resetExpiredQuotas() {
        OffsetDateTime now = OffsetDateTime.now();
        List<TenantQuota> quotas = quotaRepository.findAll();
        int resetCount = 0;
        for (TenantQuota quota : quotas) {
            if (!quota.getResetAt().isAfter(now)) {
                quotaRepository.resetQuota(
                        quota.getTenantId(),
                        now.plusMonths(1), // Next reset is one month from now
                        now);
                resetCount++;
            }
        }
        if (resetCount > 0) {
            LOG.info("operation=quota_reset status=completed count={}", resetCount);
        }
    }
}
