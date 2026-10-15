package in.fixna.platform.hospitality.observability.dto;

import java.util.Map;

/**
 * Aggregated platform health: overall status, per-component status, and release metadata.
 */
public record PlatformHealthResponse(
        String status,
        String version,
        String deployedAt,
        String environment,
        String service,
        java.time.OffsetDateTime timestamp,
        Map<String, ComponentHealthView> components) {
}