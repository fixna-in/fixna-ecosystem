package in.fixna.platform.rbac;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RbacTest {

    @Test
    void platformAdminHasAllPermissions() {
        for (Permission permission : Permission.values()) {
            assertThat(RolePermissions.has(Role.PLATFORM_ADMIN, permission)).isTrue();
        }
    }

    @Test
    void consultantAdminCanManageClients() {
        assertThat(RolePermissions.has(Role.CONSULTANT_ADMIN, Permission.CLIENT_MANAGE)).isTrue();
        assertThat(RolePermissions.has(Role.CONSULTANT_ADMIN, Permission.AI_USE)).isTrue();
    }

    @Test
    void clientAdminCanApproveProposalsButNotManage() {
        assertThat(RolePermissions.has(Role.CLIENT_ADMIN, Permission.PROPOSAL_APPROVE)).isTrue();
        assertThat(RolePermissions.has(Role.CLIENT_ADMIN, Permission.PROPOSAL_MANAGE)).isFalse();
    }

    @Test
    void clientUserIsViewOnly() {
        assertThat(RolePermissions.has(Role.CLIENT_USER, Permission.CLIENT_VIEW)).isTrue();
        assertThat(RolePermissions.has(Role.CLIENT_USER, Permission.CLIENT_MANAGE)).isFalse();
        assertThat(RolePermissions.has(Role.CLIENT_USER, Permission.PROPOSAL_APPROVE)).isFalse();
    }

    @Test
    void internalTeamCanManageTasksOnly() {
        assertThat(RolePermissions.has(Role.INTERNAL_TEAM, Permission.TASK_MANAGE)).isTrue();
        assertThat(RolePermissions.has(Role.INTERNAL_TEAM, Permission.CLIENT_MANAGE)).isFalse();
    }
}
