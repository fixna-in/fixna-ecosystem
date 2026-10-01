package in.fixna.platform.consulting.publicsite;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.profile.ConsultingProfile;
import in.fixna.platform.consulting.profile.ConsultingProfileRepository;
import in.fixna.platform.consulting.publicsite.dto.PublicWebsiteResponse;
import in.fixna.platform.consulting.service.ConsultingService;
import in.fixna.platform.consulting.service.ConsultingServiceRepository;
import in.fixna.platform.consulting.testimonial.Testimonial;
import in.fixna.platform.consulting.testimonial.TestimonialRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicWebsiteServiceTest {

    private final UUID tenantId = UUID.randomUUID();

    @Mock ConsultingProfileRepository profiles;
    @Mock ConsultingServiceRepository services;
    @Mock TestimonialRepository testimonials;

    @InjectMocks PublicWebsiteService service;

    @Test
    void unpublishedSiteReturnsNotFound() {
        when(profiles.findByPublicSlugAndPublishedTrue("hidden")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("hidden"))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void publishedSiteAssemblesContent() {
        ConsultingProfile profile = new ConsultingProfile();
        profile.setId(UUID.randomUUID());
        profile.setTenantId(tenantId);
        profile.setDisplayName("Acme Consulting");
        profile.setTagline("Strategy that scales");
        profile.setPublicSlug("acme");
        profile.setPublished(true);

        ConsultingService serviceItem = new ConsultingService();
        serviceItem.setTenantId(tenantId);
        serviceItem.setName("Discovery Workshop");
        serviceItem.setDescription("A focused half-day session");
        serviceItem.setPriceAmount(new BigDecimal("2500.00"));
        serviceItem.setPriceCurrency("USD");
        serviceItem.setActive(true);
        serviceItem.setSortOrder(1);

        Testimonial testimonial = new Testimonial();
        testimonial.setTenantId(tenantId);
        testimonial.setAuthorName("Jane Doe");
        testimonial.setAuthorTitle("CEO, Example Co");
        testimonial.setQuote("Outstanding results.");
        testimonial.setPublished(true);
        testimonial.setSortOrder(0);

        when(profiles.findByPublicSlugAndPublishedTrue("acme")).thenReturn(Optional.of(profile));
        when(services.findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(tenantId))
                .thenReturn(List.of(serviceItem));
        when(testimonials.findByTenantIdAndPublishedTrueOrderBySortOrderAscAuthorNameAsc(tenantId))
                .thenReturn(List.of(testimonial));

        PublicWebsiteResponse response = service.getBySlug("acme");

        assertThat(response.profile().displayName()).isEqualTo("Acme Consulting");
        assertThat(response.profile().publicSlug()).isEqualTo("acme");
        assertThat(response.services()).hasSize(1);
        assertThat(response.services().get(0).name()).isEqualTo("Discovery Workshop");
        assertThat(response.testimonials()).hasSize(1);
        assertThat(response.testimonials().get(0).authorName()).isEqualTo("Jane Doe");
    }
}
