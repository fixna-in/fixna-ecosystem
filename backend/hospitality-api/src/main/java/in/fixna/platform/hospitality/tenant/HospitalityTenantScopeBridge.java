package in.fixna.platform.hospitality.tenant;

import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import in.fixna.platform.common.logging.LoggingContext;

@Configuration
public class HospitalityTenantScopeBridge implements in.fixna.platform.common.tenant.TenantScopeProvider {

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