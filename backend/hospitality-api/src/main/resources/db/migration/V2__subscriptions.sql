-- V2: subscriptions table for billing (from fixna-platform-common).
-- The shared Subscription entity is on the hospitality component-scan path
-- (in.fixna.platform.billing), so Hibernate validates this table at startup
-- (ddl-auto: validate). Hospitality has no billing flows of its own; this table
-- is inert infrastructure for the shared entity.
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) UNIQUE,
    plan_code VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_subscriptions_tenant ON subscriptions(tenant_id);