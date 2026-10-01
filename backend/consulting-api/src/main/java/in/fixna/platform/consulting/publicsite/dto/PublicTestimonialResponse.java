package in.fixna.platform.consulting.publicsite.dto;

import in.fixna.platform.consulting.testimonial.Testimonial;

public record PublicTestimonialResponse(String authorName, String authorTitle, String quote, int sortOrder) {

    public static PublicTestimonialResponse from(Testimonial testimonial) {
        return new PublicTestimonialResponse(
                testimonial.getAuthorName(),
                testimonial.getAuthorTitle(),
                testimonial.getQuote(),
                testimonial.getSortOrder());
    }
}
