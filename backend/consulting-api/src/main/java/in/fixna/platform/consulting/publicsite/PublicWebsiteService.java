package in.fixna.platform.consulting.publicsite;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.profile.ConsultingProfile;
import in.fixna.platform.consulting.profile.ConsultingProfileRepository;
import in.fixna.platform.consulting.publicsite.dto.PublicProfileResponse;
import in.fixna.platform.consulting.publicsite.dto.PublicServiceResponse;
import in.fixna.platform.consulting.publicsite.dto.PublicTestimonialResponse;
import in.fixna.platform.consulting.publicsite.dto.PublicWebsiteResponse;
import in.fixna.platform.consulting.service.ConsultingServiceRepository;
import in.fixna.platform.consulting.testimonial.TestimonialRepository;

@Service
public class PublicWebsiteService {

    private final ConsultingProfileRepository profiles;
    private final ConsultingServiceRepository services;
    private final TestimonialRepository testimonials;

    public PublicWebsiteService(
            ConsultingProfileRepository profiles,
            ConsultingServiceRepository services,
            TestimonialRepository testimonials) {
        this.profiles = profiles;
        this.services = services;
        this.testimonials = testimonials;
    }

    @Transactional(readOnly = true)
    public PublicWebsiteResponse getBySlug(String slug) {
        ConsultingProfile profile = profiles.findByPublicSlugAndPublishedTrue(slug)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Published site not found"));

        return new PublicWebsiteResponse(
                PublicProfileResponse.from(profile),
                services.findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(profile.getTenantId()).stream()
                        .map(PublicServiceResponse::from)
                        .toList(),
                testimonials
                        .findByTenantIdAndPublishedTrueOrderBySortOrderAscAuthorNameAsc(profile.getTenantId())
                        .stream()
                        .map(PublicTestimonialResponse::from)
                        .toList());
    }
}
