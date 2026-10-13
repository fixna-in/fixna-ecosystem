package in.fixna.platform.audit;

import java.util.Map;
import java.util.Set;

/**
 * Utility for sanitizing audit payloads to prevent sensitive data leakage.
 * Ensures no document content, PII, or financial details are logged.
 */
public final class AuditSanitizer {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "field_value", "raw_snippet", "money_amount", "currency",
            "ocr_text", "bank_account", "tax_id", "ssn", "credit_card",
            "password", "secret", "token", "key", "authorization");

    private AuditSanitizer() {}

    /**
     * Sanitizes a map of audit details by removing or masking sensitive keys.
     * @param details the original audit details map
     * @return a new map with sensitive values replaced by "[REDACTED]"
     */
    public static Map<String, String> sanitize(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }
        var sanitized = new java.util.HashMap<String, String>(details.size());
        for (var entry : details.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (SENSITIVE_KEYS.contains(key)) {
                sanitized.put(entry.getKey(), "[REDACTED]");
            } else {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(sanitized);
    }

    /**
     * Creates a safe audit details map with only non-sensitive information.
     * @param safeDetails map of known-safe key-value pairs
     * @return the same map (caller ensures no sensitive data)
     */
    public static Map<String, String> safe(Map<String, String> safeDetails) {
        return safeDetails != null ? Map.copyOf(safeDetails) : Map.of();
    }
}