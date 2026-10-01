package in.fixna.platform.consulting.testimonial;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.testimonial.dto.TestimonialRequest;
import in.fixna.platform.consulting.testimonial.dto.TestimonialResponse;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;

@Service
public class TestimonialApplicationService {

    private final TestimonialRepository testimonials;
    private final AuditPublisher audit;

    public TestimonialApplicationService(TestimonialRepository testimonials, AuditPublisher audit) {
        this.testimonials = testimonials;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<TestimonialResponse> list() {
        requireViewPermission();
        UUID tenantId = TenantContext.requireTenantId();
        return testimonials.findByTenantIdOrderBySortOrderAscAuthorNameAsc(tenantId).stream()
                .map(TestimonialResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TestimonialResponse get(UUID id) {
        requireViewPermission();
        return TestimonialResponse.from(requireTestimonial(id));
    }

    @Transactional
    public TestimonialResponse create(TestimonialRequest request) {
        TenantContext.requirePermission(Permission.TESTIMONIAL_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        Testimonial testimonial = new Testimonial();
        testimonial.setTenantId(tenantId);
        applyFields(testimonial, request);
        testimonials.save(testimonial);

        audit.publish(new AuditEvent(
                "testimonial.created",
                tenantId,
                TenantContext.requireUserId(),
                "testimonial",
                testimonial.getId().toString(),
                Map.of("authorName", testimonial.getAuthorName()),
                null));
        return TestimonialResponse.from(testimonial);
    }

    @Transactional
    public TestimonialResponse update(UUID id, TestimonialRequest request) {
        TenantContext.requirePermission(Permission.TESTIMONIAL_MANAGE);
        Testimonial testimonial = requireTestimonial(id);
        applyFields(testimonial, request);
        testimonials.save(testimonial);

        audit.publish(new AuditEvent(
                "testimonial.updated",
                testimonial.getTenantId(),
                TenantContext.requireUserId(),
                "testimonial",
                testimonial.getId().toString(),
                Map.of("authorName", testimonial.getAuthorName()),
                null));
        return TestimonialResponse.from(testimonial);
    }

    @Transactional
    public void delete(UUID id) {
        TenantContext.requirePermission(Permission.TESTIMONIAL_MANAGE);
        Testimonial testimonial = requireTestimonial(id);
        testimonials.delete(testimonial);

        audit.publish(new AuditEvent(
                "testimonial.deleted",
                testimonial.getTenantId(),
                TenantContext.requireUserId(),
                "testimonial",
                testimonial.getId().toString(),
                Map.of(),
                null));
    }

    private void requireViewPermission() {
        if (!TenantContext.hasPermission(Permission.TESTIMONIAL_MANAGE)
                && !TenantContext.hasPermission(Permission.CLIENT_VIEW)) {
            throw new FixnaException("FORBIDDEN", HttpStatus.FORBIDDEN, "Permission denied");
        }
    }

    private Testimonial requireTestimonial(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return testimonials.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Testimonial not found"));
    }

    private static void applyFields(Testimonial testimonial, TestimonialRequest request) {
        testimonial.setAuthorName(request.authorName());
        testimonial.setAuthorTitle(request.authorTitle());
        testimonial.setQuote(request.quote());
        testimonial.setPublished(request.published());
        testimonial.setSortOrder(request.sortOrder());
    }
}
