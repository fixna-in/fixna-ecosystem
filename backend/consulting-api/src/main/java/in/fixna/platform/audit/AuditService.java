package in.fixna.platform.audit;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuditService {

    private final AuditLogRepository logs;
    private final ObjectMapper mapper;

    public AuditService(AuditLogRepository logs, ObjectMapper mapper) {
        this.logs = logs;
        this.mapper = mapper;
    }

    @Transactional
    public void record(
            UUID tenantId,
            UUID userId,
            String action,
            String resourceType,
            String resourceId,
            Map<String, String> metadata,
            OffsetDateTime occurredAt) {
        AuditLog row = new AuditLog();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setAction(action);
        row.setResourceType(resourceType);
        row.setResourceId(resourceId);
        row.setMetadata(toJson(metadata));
        row.setCreatedAt(occurredAt == null ? OffsetDateTime.now() : occurredAt);
        logs.save(row);
    }

    private String toJson(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return mapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
