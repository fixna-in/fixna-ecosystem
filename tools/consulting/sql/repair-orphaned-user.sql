-- LOCAL ONLY. Creates a new Consulting workspace for an existing user with no tenant membership.
\set ON_ERROR_STOP on

SELECT current_database() = 'consulting'
   AND inet_server_addr() IN ('127.0.0.1'::inet, '::1'::inet) AS local_database
\gset
\if :local_database
\else
  \echo 'Refusing repair: connect to consulting on local PostgreSQL.'
  \quit 1
\endif

SELECT length(trim(:'workspace_name')) BETWEEN 1 AND 200 AS valid_workspace_name
\gset
\if :valid_workspace_name
\else
  \echo 'Refusing repair: workspace_name must contain 1 to 200 characters.'
  \quit 1
\endif

SELECT id AS repair_user_id
FROM users
WHERE lower(email) = lower(:'user_email')
\gset
\if :{?repair_user_id}
\else
  \echo 'Refusing repair: user_email was not found.'
  \quit 1
\endif

SELECT NOT EXISTS (
    SELECT 1 FROM tenant_memberships WHERE user_id = :'repair_user_id'::uuid
) AS has_no_memberships
\gset
\if :has_no_memberships
\else
  \echo 'Refusing repair: user already has a tenant membership.'
  \quit 1
\endif

BEGIN;
WITH new_tenant AS (
    INSERT INTO tenants (name, tenant_type)
    VALUES (trim(:'workspace_name'), 'CONSULTING_WORKSPACE')
    RETURNING id
)
INSERT INTO tenant_memberships (tenant_id, user_id, role)
SELECT id, :'repair_user_id'::uuid, 'CONSULTANT_ADMIN'
FROM new_tenant;
COMMIT;

\echo 'Workspace membership created. Retry login with the existing account.'