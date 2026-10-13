-- V6: standalone auth for Invoice Assistant (register/login/refresh + profile).
-- Adds the profile columns the auth flow persists and the refresh token
-- rotation table (mirrors platform-core in other products).

ALTER TABLE tenants ADD COLUMN tenant_type VARCHAR(30);
ALTER TABLE users ADD COLUMN first_name VARCHAR(100);
ALTER TABLE users ADD COLUMN last_name VARCHAR(100);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tenant ON refresh_tokens(tenant_id);
CREATE INDEX idx_refresh_expires ON refresh_tokens(expires_at);
