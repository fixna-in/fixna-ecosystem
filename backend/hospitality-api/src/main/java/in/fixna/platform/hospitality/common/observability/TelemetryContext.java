package in.fixna.platform.hospitality.common.observability;

import org.slf4j.MDC;

import in.fixna.platform.common.logging.LoggingConstants;

/**
 * Lightweight access to MDC fields set by the request filters.
 * Returns {@code null} when the key is absent — callers format defensively.
 */
public final class TelemetryContext {

    private TelemetryContext() {}

    public static String requestId() {
        return MDC.get(LoggingConstants.REQUEST_ID);
    }

    public static String tenantId() {
        return MDC.get(LoggingConstants.TENANT_ID);
    }

    public static String userId() {
        return MDC.get(LoggingConstants.USER_ID);
    }
}