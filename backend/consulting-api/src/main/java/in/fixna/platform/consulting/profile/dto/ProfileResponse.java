package in.fixna.platform.consulting.profile.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.profile.ConsultingProfile;

public record ProfileResponse(
        UUID id,
        String displayName,
        String tagline,
        String bio,
        String websiteUrl,
        String logoUrl,
        String publicSlug,
        boolean published,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ProfileResponse from(ConsultingProfile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getDisplayName(),
                profile.getTagline(),
                profile.getBio(),
                profile.getWebsiteUrl(),
                profile.getLogoUrl(),
                profile.getPublicSlug(),
                profile.isPublished(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    public static ProfileResponse empty() {
        return new ProfileResponse(null, null, null, null, null, null, null, false, null, null);
    }
}
