package in.fixna.platform.rbac;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Static role-to-permission mapping for consulting RBAC. */
public final class RolePermissions {

    private static final Map<Role, Set<Permission>> MAPPING = Map.of(
            Role.PLATFORM_ADMIN, EnumSet.allOf(Permission.class),
            Role.CONSULTANT_ADMIN, EnumSet.of(
                    Permission.CLIENT_VIEW,
                    Permission.CLIENT_MANAGE,
                    Permission.PROJECT_VIEW,
                    Permission.PROJECT_MANAGE,
                    Permission.TASK_VIEW,
                    Permission.TASK_MANAGE,
                    Permission.MEETING_VIEW,
                    Permission.MEETING_MANAGE,
                    Permission.PROPOSAL_VIEW,
                    Permission.PROPOSAL_MANAGE,
                    Permission.PROPOSAL_APPROVE,
                    Permission.INVOICE_VIEW,
                    Permission.INVOICE_MANAGE,
                    Permission.PAYMENT_VIEW,
                    Permission.WEBSITE_MANAGE,
                    Permission.TESTIMONIAL_MANAGE,
                    Permission.AI_USE),
            Role.CONSULTANT, EnumSet.of(
                    Permission.CLIENT_VIEW,
                    Permission.CLIENT_MANAGE,
                    Permission.PROJECT_VIEW,
                    Permission.PROJECT_MANAGE,
                    Permission.TASK_VIEW,
                    Permission.TASK_MANAGE,
                    Permission.MEETING_VIEW,
                    Permission.MEETING_MANAGE,
                    Permission.PROPOSAL_VIEW,
                    Permission.PROPOSAL_MANAGE,
                    Permission.INVOICE_VIEW,
                    Permission.INVOICE_MANAGE,
                    Permission.PAYMENT_VIEW,
                    Permission.AI_USE),
            Role.INTERNAL_TEAM, EnumSet.of(
                    Permission.CLIENT_VIEW,
                    Permission.PROJECT_VIEW,
                    Permission.TASK_VIEW,
                    Permission.TASK_MANAGE,
                    Permission.MEETING_VIEW),
            Role.CLIENT_ADMIN, EnumSet.of(
                    Permission.CLIENT_VIEW,
                    Permission.PROJECT_VIEW,
                    Permission.PROPOSAL_VIEW,
                    Permission.PROPOSAL_APPROVE,
                    Permission.INVOICE_VIEW,
                    Permission.PAYMENT_VIEW,
                    Permission.MEETING_VIEW),
            Role.CLIENT_USER, EnumSet.of(
                    Permission.CLIENT_VIEW,
                    Permission.PROJECT_VIEW,
                    Permission.PROPOSAL_VIEW,
                    Permission.INVOICE_VIEW,
                    Permission.MEETING_VIEW));

    private RolePermissions() {}

    public static boolean has(Role role, Permission permission) {
        if (role == null || permission == null) {
            return false;
        }
        Set<Permission> permissions = MAPPING.get(role);
        return permissions != null && permissions.contains(permission);
    }

    public static Set<Permission> permissionsFor(Role role) {
        return MAPPING.getOrDefault(role, Set.of());
    }
}
