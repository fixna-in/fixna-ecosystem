-- Seed default fixna user and tenant
INSERT INTO tenants (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'Fixna Default Workspace')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, email, password_hash)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'fixnainfo@gmail.com',
    '$2a$10$X13s0ZqW9Z5q3ZqW9Z5q3O123456789012345678901234567890'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO tenant_memberships (tenant_id, user_id, role)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'ADMIN'
)
ON CONFLICT (tenant_id, user_id) DO NOTHING;
