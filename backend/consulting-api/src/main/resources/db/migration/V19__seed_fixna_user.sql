-- Seed default fixna user and tenant
INSERT INTO tenants (id, name, tenant_type)
VALUES ('00000000-0000-0000-0000-000000000001', 'Fixna Default Workspace', 'ORGANIZATION')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, email, password_hash, first_name, last_name)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'fixnainfo@gmail.com',
    '$2a$10$X13s0ZqW9Z5q3ZqW9Z5q3O123456789012345678901234567890',
    'Fixna',
    'Admin'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO tenant_memberships (tenant_id, user_id, role)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'TENANT_ADMIN'
)
ON CONFLICT (tenant_id, user_id) DO NOTHING;
