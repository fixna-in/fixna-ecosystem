-- LOCAL ONLY. Run with psql -X -h localhost -U postgres -d hospitality -f schema.sql
-- Snapshot of immutable Flyway migrations. Empty public schema required.
-- Do not run on an existing migrated database. Future changes use Flyway.
BEGIN;
DO $$ BEGIN
  IF inet_server_addr() IS NOT NULL AND inet_server_addr() NOT IN ('127.0.0.1'::inet, '::1'::inet) THEN
    RAISE EXCEPTION 'Local PostgreSQL only';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public') THEN
    RAISE EXCEPTION 'Public schema is not empty; use Flyway for existing databases';
  END IF;
END $$;
SET LOCAL search_path = public;

-- Source: V1__platform_core.sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    tenant_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tenant_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_membership_user ON tenant_memberships(user_id);
CREATE INDEX idx_membership_tenant ON tenant_memberships(tenant_id);

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

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(100),
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_tenant_time ON audit_logs(tenant_id, created_at);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID REFERENCES users(id),
    event VARCHAR(100) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    details JSONB,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_tenant_time ON notifications(tenant_id, created_at);

-- Future: Hospitality-specific tables will be added via Flyway migrations
-- Example placeholder structure (not executed - for reference only):
-- CREATE TABLE properties (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     tenant_id UUID NOT NULL REFERENCES tenants(id),
--     name VARCHAR(255) NOT NULL,
--     property_type VARCHAR(50) NOT NULL, -- HOTEL, RESORT, HOMESTAY, APARTMENT
--     address_line VARCHAR(500),
--     city VARCHAR(150),
--     state VARCHAR(150),
--     postal_code VARCHAR(30),
--     country VARCHAR(100) DEFAULT 'India',
--     latitude NUMERIC(10,7),
--     longitude NUMERIC(10,7),
--     description TEXT,
--     amenities JSONB,
--     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
--     updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
-- );
-- CREATE INDEX idx_properties_tenant ON properties(tenant_id);

-- CREATE TABLE rooms (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     tenant_id UUID NOT NULL REFERENCES tenants(id),
--     property_id UUID NOT NULL REFERENCES properties(id),
--     room_type VARCHAR(100) NOT NULL,
--     name VARCHAR(255),
--     base_price NUMERIC(12,2),
--     currency VARCHAR(3) DEFAULT 'INR',
--     max_occupancy INT DEFAULT 2,
--     amenities JSONB,
--     status VARCHAR(30) DEFAULT 'ACTIVE',
--     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
--     updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
-- );
-- CREATE INDEX idx_rooms_tenant ON rooms(tenant_id);
-- CREATE INDEX idx_rooms_property ON rooms(property_id);

-- CREATE TABLE bookings (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     tenant_id UUID NOT NULL REFERENCES tenants(id),
--     property_id UUID NOT NULL REFERENCES properties(id),
--     room_id UUID NOT NULL REFERENCES rooms(id),
--     guest_name VARCHAR(255) NOT NULL,
--     guest_email VARCHAR(320),
--     guest_phone VARCHAR(50),
--     check_in DATE NOT NULL,
--     check_out DATE NOT NULL,
--     status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
--     total_amount NUMERIC(12,2),
--     currency VARCHAR(3) DEFAULT 'INR',
--     source VARCHAR(50),
--     created_by_user_id UUID REFERENCES users(id),
--     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
--     updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
-- );
-- CREATE INDEX idx_bookings_tenant ON bookings(tenant_id);
-- CREATE INDEX idx_bookings_property ON bookings(property_id);
-- CREATE INDEX idx_bookings_dates ON bookings(check_in, check_out);

-- Flyway baseline marker
CREATE TABLE public.flyway_schema_history (
 installed_rank integer NOT NULL PRIMARY KEY, version varchar(50),
 description varchar(200) NOT NULL, type varchar(20) NOT NULL,
 script varchar(1000) NOT NULL, checksum integer, installed_by varchar(100) NOT NULL,
 installed_on timestamp NOT NULL DEFAULT now(), execution_time integer NOT NULL,
 success boolean NOT NULL
);
CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history(success);
INSERT INTO public.flyway_schema_history
 (installed_rank, version, description, type, script, installed_by, execution_time, success)
 VALUES (1, '1', 'Local schema snapshot V1', 'BASELINE', 'Local schema snapshot V1', current_user, 0, true);
COMMIT;