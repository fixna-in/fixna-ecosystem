package in.fixna.platform.consulting.publicsite.dto;

import in.fixna.platform.consulting.profile.ConsultingProfile;

public record PublicProfileResponse(
        String displayName,
        String tagline,
        String bio,
        String websiteUrl,
        String logoUrl,
        String publicSlug) {

    public static PublicProfileResponse from(ConsultingProfile profile) {
        return new PublicProfileResponse(
                profile.getDisplayName(),
                profile.getTagline(),
                profile.getBio(),
                profile.getWebsiteUrl(),
                profile.getLogoUrl(),
                profile.getPublicSlug());
    }
}
