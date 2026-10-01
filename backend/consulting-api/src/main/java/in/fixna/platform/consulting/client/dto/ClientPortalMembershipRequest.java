package in.fixna.platform.consulting.client.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import in.fixna.platform.rbac.Role;

public record ClientPortalMembershipRequest(@NotNull UUID userId, @NotNull Role role) {}
