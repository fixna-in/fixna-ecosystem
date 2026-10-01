package in.fixna.platform.consulting.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.service.dto.ServiceCatalogRequest;
import in.fixna.platform.consulting.service.dto.ServiceCatalogResponse;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;

@Service
public class ServiceCatalogApplicationService {

    private final ConsultingServiceRepository services;
    private final AuditPublisher audit;

    public ServiceCatalogApplicationService(ConsultingServiceRepository services, AuditPublisher audit) {
        this.services = services;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ServiceCatalogResponse> list() {
        TenantContext.requirePermission(Permission.CLIENT_VIEW);
        UUID tenantId = TenantContext.requireTenantId();
        return services.findByTenantIdOrderBySortOrderAscNameAsc(tenantId).stream()
                .map(ServiceCatalogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceCatalogResponse get(UUID id) {
        TenantContext.requirePermission(Permission.CLIENT_VIEW);
        return ServiceCatalogResponse.from(requireService(id));
    }

    @Transactional
    public ServiceCatalogResponse create(ServiceCatalogRequest request) {
        TenantContext.requirePermission(Permission.CLIENT_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        ConsultingService service = new ConsultingService();
        service.setTenantId(tenantId);
        applyFields(service, request);
        services.save(service);

        audit.publish(new AuditEvent(
                "service.created",
                tenantId,
                TenantContext.requireUserId(),
                "consulting_service",
                service.getId().toString(),
                Map.of("name", service.getName()),
                null));
        return ServiceCatalogResponse.from(service);
    }

    @Transactional
    public ServiceCatalogResponse update(UUID id, ServiceCatalogRequest request) {
        TenantContext.requirePermission(Permission.CLIENT_MANAGE);
        ConsultingService service = requireService(id);
        applyFields(service, request);
        services.save(service);

        audit.publish(new AuditEvent(
                "service.updated",
                service.getTenantId(),
                TenantContext.requireUserId(),
                "consulting_service",
                service.getId().toString(),
                Map.of("name", service.getName()),
                null));
        return ServiceCatalogResponse.from(service);
    }

    private ConsultingService requireService(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return services.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Service not found"));
    }

    private static void applyFields(ConsultingService service, ServiceCatalogRequest request) {
        service.setName(request.name());
        service.setDescription(request.description());
        service.setPriceAmount(request.priceAmount());
        service.setPriceCurrency(request.priceCurrency() == null ? "USD" : request.priceCurrency());
        service.setDurationMinutes(request.durationMinutes());
        service.setActive(request.active());
        service.setSortOrder(request.sortOrder());
    }
}
