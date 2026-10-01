package in.fixna.platform.consulting.profile.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 200) String displayName,
        @Size(max = 500) String tagline,
        String bio,
        @Size(max = 1000) String websiteUrl,
        @Size(max = 1000) String logoUrl,
        @Size(max = 100) String publicSlug,
        Boolean published) {}
