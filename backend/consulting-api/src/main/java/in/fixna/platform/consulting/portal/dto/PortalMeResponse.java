package in.fixna.platform.consulting.portal.dto;

import java.util.UUID;

import in.fixna.platform.rbac.Role;

public record PortalMeResponse(
        UUID userId, UUID tenantId, UUID clientId, String clientName, Role role) {}
