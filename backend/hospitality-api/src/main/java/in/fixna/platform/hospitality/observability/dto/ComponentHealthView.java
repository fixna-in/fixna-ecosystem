package in.fixna.platform.hospitality.observability.dto;

import java.util.Map;

/**
 * Single component health view.
 */
public record ComponentHealthView(
        String status,
        Map<String, Object> details) {
}