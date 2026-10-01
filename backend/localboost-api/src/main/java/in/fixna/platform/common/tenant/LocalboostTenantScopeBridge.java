package in.fixna.platform.common.tenant;

import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import in.fixna.platform.common.logging.LoggingContext;

@Configuration
public class LocalboostTenantScopeBridge implements TenantScopeProvider {

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
