package in.fixna.platform.hospitality.tenant;

public enum MembershipRole {
    TENANT_OWNER,
    TENANT_ADMIN,
    TENANT_EDITOR,
    TENANT_VIEWER;

    public boolean canWrite() {
        return this == TENANT_OWNER || this == TENANT_ADMIN || this == TENANT_EDITOR;
    }

    public boolean canAdminister() {
        return this == TENANT_OWNER || this == TENANT_ADMIN;
    }
}