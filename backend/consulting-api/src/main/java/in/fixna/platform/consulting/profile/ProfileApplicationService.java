package in.fixna.platform.consulting.profile;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.profile.dto.ProfileResponse;
import in.fixna.platform.consulting.profile.dto.ProfileUpdateRequest;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;

@Service
public class ProfileApplicationService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private final ConsultingProfileRepository profiles;
    private final AuditPublisher audit;

    public ProfileApplicationService(ConsultingProfileRepository profiles, AuditPublisher audit) {
        this.profiles = profiles;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public ProfileResponse get() {
        TenantContext.requirePermission(Permission.CLIENT_VIEW);
        UUID tenantId = TenantContext.requireTenantId();
        return profiles.findByTenantId(tenantId).map(ProfileResponse::from).orElseGet(ProfileResponse::empty);
    }

    @Transactional
    public ProfileResponse update(ProfileUpdateRequest request) {
        TenantContext.requirePermission(Permission.WEBSITE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        ConsultingProfile profile = profiles.findByTenantId(tenantId).orElseGet(() -> {
            ConsultingProfile created = new ConsultingProfile();
            created.setTenantId(tenantId);
            return created;
        });
        profile.setDisplayName(request.displayName());
        profile.setTagline(request.tagline());
        profile.setBio(request.bio());
        profile.setWebsiteUrl(request.websiteUrl());
        profile.setLogoUrl(request.logoUrl());

        if (request.publicSlug() != null) {
            String slug = normalizeSlug(request.publicSlug());
            if (slug != null && !SLUG_PATTERN.matcher(slug).matches()) {
                throw new FixnaException(
                        "INVALID_SLUG",
                        HttpStatus.BAD_REQUEST,
                        "Public slug must contain only lowercase letters, numbers, and hyphens");
            }
            if (slug != null && profiles.existsByPublicSlugAndTenantIdNot(slug, tenantId)) {
                throw new FixnaException("SLUG_TAKEN", HttpStatus.CONFLICT, "Public slug is already in use");
            }
            profile.setPublicSlug(slug);
        }

        if (request.published() != null) {
            boolean publish = request.published();
            String effectiveSlug = profile.getPublicSlug();
            if (publish && (effectiveSlug == null || effectiveSlug.isBlank())) {
                throw new FixnaException(
                        "PUBLISH_REQUIRES_SLUG",
                        HttpStatus.BAD_REQUEST,
                        "A public slug is required to publish the website");
            }
            profile.setPublished(publish);
        }

        profiles.save(profile);

        audit.publish(new AuditEvent(
                "profile.updated",
                tenantId,
                TenantContext.requireUserId(),
                "consulting_profile",
                profile.getId().toString(),
                Map.of("published", String.valueOf(profile.isPublished())),
                null));
        return ProfileResponse.from(profile);
    }

    private static String normalizeSlug(String slug) {
        if (slug == null) {
            return null;
        }
        String trimmed = slug.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
