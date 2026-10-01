package in.fixna.platform.consulting.testimonial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestimonialRequest(
        @NotBlank @Size(max = 200) String authorName,
        @Size(max = 200) String authorTitle,
        @NotBlank String quote,
        boolean published,
        int sortOrder) {}
