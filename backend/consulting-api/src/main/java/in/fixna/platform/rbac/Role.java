package in.fixna.platform.rbac;

/** Consulting workspace roles. Permissions are resolved via {@link RolePermissions}. */
public enum Role {
    PLATFORM_ADMIN,
    CONSULTANT_ADMIN,
    CONSULTANT,
    INTERNAL_TEAM,
    CLIENT_ADMIN,
    CLIENT_USER
}
