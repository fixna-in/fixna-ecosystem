package in.fixna.platform.consulting.engagement;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.client.ClientRepository;
import in.fixna.platform.consulting.dto.StatusTransitionRequest;
import in.fixna.platform.consulting.engagement.dto.EngagementRequest;
import in.fixna.platform.consulting.engagement.dto.EngagementResponse;
import in.fixna.platform.consulting.engagement.event.EngagementCreated;
import in.fixna.platform.consulting.engagement.event.EngagementStatusChanged;
import in.fixna.platform.event.DomainEventPublisher;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;

@Service
public class EngagementApplicationService {

    private final EngagementRepository engagements;
    private final ClientRepository clients;
    private final DomainEventPublisher events;
    private final AuditPublisher audit;

    public EngagementApplicationService(
            EngagementRepository engagements,
            ClientRepository clients,
            DomainEventPublisher events,
            AuditPublisher audit) {
        this.engagements = engagements;
        this.clients = clients;
        this.events = events;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<EngagementResponse> listByClient(UUID clientId) {
        TenantContext.requirePermission(Permission.PROJECT_VIEW);
        requireClient(clientId);
        UUID tenantId = TenantContext.requireTenantId();
        return engagements.findByClientIdAndTenantIdOrderByCreatedAtDesc(clientId, tenantId).stream()
                .map(EngagementResponse::from)
                .toList();
    }

    @Transactional
    public EngagementResponse create(UUID clientId, EngagementRequest request) {
        TenantContext.requirePermission(Permission.PROJECT_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        requireClient(clientId);

        Engagement engagement = new Engagement();
        engagement.setTenantId(tenantId);
        engagement.setClientId(clientId);
        applyFields(engagement, request);
        engagements.save(engagement);

        events.publish(new EngagementCreated(engagement.getId(), tenantId));
        audit.publish(new AuditEvent(
                "engagement.created",
                tenantId,
                TenantContext.requireUserId(),
                "engagement",
                engagement.getId().toString(),
                Map.of("title", engagement.getTitle(), "clientId", clientId.toString()),
                null));
        return EngagementResponse.from(engagement);
    }

    @Transactional(readOnly = true)
    public EngagementResponse get(UUID id) {
        TenantContext.requirePermission(Permission.PROJECT_VIEW);
        return EngagementResponse.from(requireEngagement(id));
    }

    @Transactional
    public EngagementResponse update(UUID id, EngagementRequest request) {
        TenantContext.requirePermission(Permission.PROJECT_MANAGE);
        Engagement engagement = requireEngagement(id);
        applyFields(engagement, request);
        engagements.save(engagement);

        audit.publish(new AuditEvent(
                "engagement.updated",
                engagement.getTenantId(),
                TenantContext.requireUserId(),
                "engagement",
                engagement.getId().toString(),
                Map.of("title", engagement.getTitle()),
                null));
        return EngagementResponse.from(engagement);
    }

    @Transactional
    public EngagementResponse transition(UUID id, StatusTransitionRequest request) {
        TenantContext.requirePermission(Permission.PROJECT_MANAGE);
        Engagement engagement = requireEngagement(id);
        EngagementStatus fromStatus = engagement.getStatus();
        EngagementStatus toStatus = parseEngagementStatus(request.status());
        fromStatus.validateTransitionTo(toStatus);
        engagement.setStatus(toStatus);
        engagements.save(engagement);

        events.publish(new EngagementStatusChanged(
                engagement.getId(), engagement.getTenantId(), fromStatus, toStatus));
        audit.publish(new AuditEvent(
                "engagement.status_changed",
                engagement.getTenantId(),
                TenantContext.requireUserId(),
                "engagement",
                engagement.getId().toString(),
                Map.of("fromStatus", fromStatus.name(), "toStatus", toStatus.name()),
                null));
        return EngagementResponse.from(engagement);
    }

    public Engagement requireEngagement(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return engagements.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Engagement not found"));
    }

    private void requireClient(UUID clientId) {
        UUID tenantId = TenantContext.requireTenantId();
        clients.findByIdAndTenantId(clientId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Client not found"));
    }

    private static void applyFields(Engagement engagement, EngagementRequest request) {
        engagement.setTitle(request.title());
        engagement.setDescription(request.description());
        engagement.setStartDate(request.startDate());
        engagement.setEndDate(request.endDate());
    }

    private static EngagementStatus parseEngagementStatus(String raw) {
        try {
            return EngagementStatus.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new FixnaException(
                    "INVALID_STATUS", HttpStatus.BAD_REQUEST, "Unknown engagement status: " + raw);
        }
    }
}
