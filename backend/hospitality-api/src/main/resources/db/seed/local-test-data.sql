-- Local test fixture only; executed by LocalTestDataSeeder after Flyway.
-- :passwordHash is bound by JDBC, never interpolated or stored as plaintext.
-- All rows are inserted atomically by this single PostgreSQL statement.
-- An existing email skips the entire fixture, including memberships.
WITH new_user AS (
    INSERT INTO users (id, email, password_hash, first_name, last_name)
    VALUES (:userId, 'owner@example.com', :passwordHash, 'Test', 'Owner')
    ON CONFLICT (email) DO NOTHING
    RETURNING id
), new_tenant AS (
    INSERT INTO tenants (id, name, tenant_type)
    VALUES (:tenantId, 'Fixna Demo Hospitality', 'HOSPITALITY')
    RETURNING id
), new_membership AS (
    INSERT INTO tenant_memberships (id, tenant_id, user_id, role)
    VALUES (:membershipId, (SELECT id FROM new_tenant), (SELECT id FROM new_user), 'TENANT_OWNER')
)
SELECT 1;