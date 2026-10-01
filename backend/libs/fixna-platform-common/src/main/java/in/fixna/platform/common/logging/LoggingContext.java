package in.fixna.platform.common.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;

import in.fixna.platform.common.tenant.TenantScopeProvider;

/**
 * Central helper for MDC-based contextual logging shared across Fixna products.
 */
public final class LoggingContext {

    private static volatile TenantScopeProvider tenantScope = new NoOpTenantScope();

    private LoggingContext() {}

    public static void registerTenantScope(TenantScopeProvider provider) {
        tenantScope = provider != null ? provider : new NoOpTenantScope();
    }

    public static void putRequestId(String requestId) {
        putIfPresent(LoggingConstants.REQUEST_ID, requestId);
    }

    public static void putTrace(String traceId, String spanId) {
        putIfPresent(LoggingConstants.TRACE_ID, traceId);
        putIfPresent(LoggingConstants.SPAN_ID, spanId);
    }

    public static void putTenantAndUser() {
        UUID tenant = tenantScope.tenantIdOrNull();
        UUID user = tenantScope.userIdOrNull();
        putIfPresent(LoggingConstants.TENANT_ID, tenant == null ? null : tenant.toString());
        putIfPresent(LoggingConstants.USER_ID, user == null ? null : user.toString());
    }

    public static void putTenantAndUser(UUID tenantId, UUID userId) {
        putIfPresent(LoggingConstants.TENANT_ID, tenantId == null ? null : tenantId.toString());
        putIfPresent(LoggingConstants.USER_ID, userId == null ? null : userId.toString());
    }

    public static void putContextKey(String key, UUID value) {
        putIfPresent(key, value == null ? null : value.toString());
    }

    public static void putContextKey(String key, String value) {
        putIfPresent(key, value);
    }

    public static void putCampaignId(UUID campaignId) {
        putContextKey(LoggingConstants.CAMPAIGN_ID, campaignId);
    }

    public static void putCampaignId(String campaignId) {
        putContextKey(LoggingConstants.CAMPAIGN_ID, campaignId);
    }

    public static void putOperation(String operation) {
        putIfPresent(LoggingConstants.OPERATION, operation);
    }

    public static Map<String, String> snapshot() {
        Map<String, String> copy = MDC.getCopyOfContextMap();
        return copy == null ? Map.of() : new HashMap<>(copy);
    }

    public static void restore(Map<String, String> snapshot) {
        MDC.clear();
        if (snapshot != null && !snapshot.isEmpty()) {
            MDC.setContextMap(snapshot);
        }
    }

    public static void clearRequest() {
        MDC.remove(LoggingConstants.TRACE_ID);
        MDC.remove(LoggingConstants.SPAN_ID);
        MDC.remove(LoggingConstants.REQUEST_ID);
        MDC.remove(LoggingConstants.TENANT_ID);
        MDC.remove(LoggingConstants.USER_ID);
        MDC.remove(LoggingConstants.CAMPAIGN_ID);
        MDC.remove(LoggingConstants.OPERATION);
    }

    private static void putIfPresent(String key, String value) {
        if (value == null || value.isBlank()) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, value);
    }

    private static final class NoOpTenantScope implements TenantScopeProvider {
        @Override
        public UUID tenantIdOrNull() {
            return null;
        }

        @Override
        public UUID userIdOrNull() {
            return null;
        }
    }
}
