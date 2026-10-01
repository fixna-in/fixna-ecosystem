ALTER TABLE refresh_tokens
    ADD COLUMN client_id UUID REFERENCES clients(id);

CREATE INDEX IF NOT EXISTS idx_client_portal_memberships_user ON client_portal_memberships(user_id);
