package in.fixna.platform.consulting.testimonial.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.testimonial.Testimonial;

public record TestimonialResponse(
        UUID id,
        String authorName,
        String authorTitle,
        String quote,
        boolean published,
        int sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static TestimonialResponse from(Testimonial testimonial) {
        return new TestimonialResponse(
                testimonial.getId(),
                testimonial.getAuthorName(),
                testimonial.getAuthorTitle(),
                testimonial.getQuote(),
                testimonial.isPublished(),
                testimonial.getSortOrder(),
                testimonial.getCreatedAt(),
                testimonial.getUpdatedAt());
    }
}
