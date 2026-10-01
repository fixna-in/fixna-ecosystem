package in.fixna.platform.consulting.publicsite.dto;

import java.util.List;

public record PublicWebsiteResponse(
        PublicProfileResponse profile,
        List<PublicServiceResponse> services,
        List<PublicTestimonialResponse> testimonials) {}
