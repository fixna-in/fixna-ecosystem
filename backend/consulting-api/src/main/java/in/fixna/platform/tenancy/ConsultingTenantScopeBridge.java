package in.fixna.platform.tenancy;

import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import in.fixna.platform.common.logging.LoggingContext;
import in.fixna.platform.common.tenant.TenantScopeProvider;

@Configuration
public class ConsultingTenantScopeBridge implements TenantScopeProvider {

    @PostConstruct
    void register() {
        LoggingContext.registerTenantScope(this);
    }

    @Override
    public UUID tenantIdOrNull() {
        try {
            return TenantContext.requireTenantId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @Override
    public UUID userIdOrNull() {
        try {
            return TenantContext.requireUserId();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
