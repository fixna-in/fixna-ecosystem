package in.fixna.platform.consulting.testimonial;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.testimonial.dto.TestimonialRequest;
import in.fixna.platform.consulting.testimonial.dto.TestimonialResponse;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestimonialApplicationServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID otherTenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Mock TestimonialRepository testimonials;
    @Mock AuditPublisher audit;

    @InjectMocks TestimonialApplicationService service;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private void asConsultantAdmin() {
        TenantContext.set(tenantId, userId, Role.CONSULTANT_ADMIN);
    }

    private void asConsultant() {
        TenantContext.set(tenantId, userId, Role.CONSULTANT);
    }

    @Test
    void listRequiresViewPermission() {
        asConsultant();
        when(testimonials.findByTenantIdOrderBySortOrderAscAuthorNameAsc(tenantId)).thenReturn(List.of());

        assertThat(service.list()).isEmpty();
    }

    @Test
    void consultantCannotManageTestimonials() {
        asConsultant();

        assertThatThrownBy(() -> service.create(new TestimonialRequest("Jane", "CEO", "Great work", true, 0)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createPersistsTestimonial() {
        asConsultantAdmin();
        when(testimonials.save(any(Testimonial.class))).thenAnswer(invocation -> {
            Testimonial testimonial = invocation.getArgument(0);
            testimonial.setId(UUID.randomUUID());
            return testimonial;
        });

        TestimonialResponse response = service.create(
                new TestimonialRequest("Jane Doe", "CEO", "Excellent partner", true, 1));

        assertThat(response.authorName()).isEqualTo("Jane Doe");
        assertThat(response.published()).isTrue();
        verify(audit).publish(any());
    }

    @Test
    void updateEnforcesTenantIsolation() {
        asConsultantAdmin();
        UUID testimonialId = UUID.randomUUID();
        when(testimonials.findByIdAndTenantId(testimonialId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                        testimonialId, new TestimonialRequest("Jane", null, "Updated quote", false, 0)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void updateChangesFieldsForOwnTenant() {
        asConsultantAdmin();
        UUID testimonialId = UUID.randomUUID();
        Testimonial existing = new Testimonial();
        existing.setId(testimonialId);
        existing.setTenantId(tenantId);
        existing.setAuthorName("Old Name");
        existing.setQuote("Old quote");
        when(testimonials.findByIdAndTenantId(testimonialId, tenantId)).thenReturn(Optional.of(existing));
        when(testimonials.save(any(Testimonial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestimonialResponse response = service.update(
                testimonialId, new TestimonialRequest("New Name", "CTO", "New quote", true, 2));

        assertThat(response.authorName()).isEqualTo("New Name");
        assertThat(response.sortOrder()).isEqualTo(2);
    }

    @Test
    void deleteRemovesTestimonial() {
        asConsultantAdmin();
        UUID testimonialId = UUID.randomUUID();
        Testimonial existing = new Testimonial();
        existing.setId(testimonialId);
        existing.setTenantId(tenantId);
        when(testimonials.findByIdAndTenantId(testimonialId, tenantId)).thenReturn(Optional.of(existing));

        service.delete(testimonialId);

        verify(testimonials).delete(existing);
        verify(audit).publish(any());
    }

    @Test
    void cannotAccessOtherTenantTestimonial() {
        asConsultantAdmin();
        UUID testimonialId = UUID.randomUUID();
        Testimonial otherTenantTestimonial = new Testimonial();
        otherTenantTestimonial.setId(testimonialId);
        otherTenantTestimonial.setTenantId(otherTenantId);
        when(testimonials.findByIdAndTenantId(testimonialId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(testimonialId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("NOT_FOUND");
    }
}
