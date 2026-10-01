package in.fixna.platform.rbac;

/** Granular permissions enforced via {@link in.fixna.platform.tenancy.TenantContext}. */
public enum Permission {
    CLIENT_VIEW,
    CLIENT_MANAGE,
    PROJECT_VIEW,
    PROJECT_MANAGE,
    TASK_VIEW,
    TASK_MANAGE,
    MEETING_VIEW,
    MEETING_MANAGE,
    PROPOSAL_VIEW,
    PROPOSAL_MANAGE,
    PROPOSAL_APPROVE,
    INVOICE_VIEW,
    INVOICE_MANAGE,
    PAYMENT_VIEW,
    WEBSITE_MANAGE,
    TESTIMONIAL_MANAGE,
    AI_USE
}
