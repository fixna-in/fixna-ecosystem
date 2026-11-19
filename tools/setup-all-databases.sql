-- Fixna Ecosystem — Database Initialization Script for Dokploy / PostgreSQL
-- Run this script once as superuser (e.g. postgres) in your PostgreSQL instance.
-- Naming Convention: Database: <app>_db, Login Role: <app>_user
-- Host: fixnainfra-postgresadmin-qfasaa, Port: 5432

SELECT 'Creating databases and users...' AS status;

DO $$
BEGIN
    -- Create consulting database and user with idempotent checks
    EXECUTE $$
        CREATE DATABASE IF NOT EXISTS consulting_db;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Database consulting_db already exists';
    END $$;

    EXECUTE $$
        CREATE USER IF NOT EXISTS consulting_user WITH PASSWORD 'change-me-secure-password';
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'User consulting_user already exists';
    END $$;

    EXECUTE $$
        GRANT ALL PRIVILEGES ON DATABASE consulting_db TO consulting_user;
    EXCEPTION WHEN duplicate_privilege ON NULL THEN
        RAISE NOTICE 'Privileges already granted';
    END $$;

    EXECUTE $$
        ALTER DATABASE consulting_db OWNER TO consulting_user;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Owner already set';
    END $$;

    EXECUTE $$
        GRANT ALL ON SCHEMA public TO consulting_user;
    EXCEPTION WHEN duplicate_grant THEN
        RAISE NOTICE 'Grant already exists';
    END $$;

    EXECUTE $$
        ALTER SCHEMA public OWNER TO consulting_user;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Schema owner already set';
    END $$;
END $$;

DO $$
BEGIN
    -- Create localboost database and user with idempotent checks
    EXECUTE $$
        CREATE DATABASE IF NOT EXISTS localboost_db;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Database localboost_db already exists';
    END $$;

    EXECUTE $$
        CREATE USER IF NOT EXISTS localboost_user WITH PASSWORD 'change-me-secure-password';
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'User localboost_user already exists';
    END $$;

    EXECUTE $$
        GRANT ALL PRIVILEGES ON DATABASE localboost_db TO localboost_user;
    EXCEPTION WHEN duplicate_privilege ON NULL THEN
        RAISE NOTICE 'Privileges already granted';
    END $$;

    EXECUTE $$
        ALTER DATABASE localboost_db OWNER TO localboost_user;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Owner already set';
    END $$;

    EXECUTE $$
        GRANT ALL ON SCHEMA public TO localboost_user;
    EXCEPTION WHEN duplicate_grant THEN
        RAISE NOTICE 'Grant already exists';
    END $$;

    EXECUTE $$
        ALTER SCHEMA public OWNER TO localboost_user;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Schema owner already set';
    END $$;
END $$;

DO $$
BEGIN
    -- Create website_audit database and user with idempotent checks
    EXECUTE $$
        CREATE DATABASE IF NOT EXISTS website_audit_db;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Database website_audit_db already exists';
    END $$;

    EXECUTE $$
        CREATE USER IF NOT EXISTS website_audit_user WITH PASSWORD 'change-me-secure-password';
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'User website_audit_user already exists';
    END $$;

    EXECUTE $$
        GRANT ALL PRIVILEGES ON DATABASE website_audit_db TO website_audit_user;
    EXCEPTION WHEN duplicate_privilege ON NULL THEN
        RAISE NOTICE 'Privileges already granted';
    END $$;

    EXECUTE $$
        ALTER DATABASE website_audit_db OWNER TO website_audit_user;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Owner already set';
    END $$;

    EXECUTE $$
        GRANT ALL ON SCHEMA public TO website_audit_user;
    EXCEPTION WHEN duplicate_grant THEN
        RAISE NOTICE 'Grant already exists';
    END $$;

    EXECUTE $$
        ALTER SCHEMA public OWNER TO website_audit_user;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Schema owner already set';
    END $$;
END $$;

DO $$
BEGIN
    -- Create invoice_assistant database and user with idempotent checks
    EXECUTE $$
        CREATE DATABASE IF NOT EXISTS invoice_assistant_db;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Database invoice_assistant_db already exists';
    END $$;

    EXECUTE $$
        CREATE USER IF NOT EXISTS invoice_assistant_user WITH PASSWORD 'change-me-secure-password';
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'User invoice_assistant_user already exists';
    END $$;

    EXECUTE $$
        GRANT ALL PRIVILEGES ON DATABASE invoice_assistant_db TO invoice_assistant_user;
    EXCEPTION WHEN duplicate_privilege ON NULL THEN
        RAISE NOTICE 'Privileges already granted';
    END $$;

    EXECUTE $$
        ALTER DATABASE invoice_assistant_db OWNER TO invoice_assistant_user;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Owner already set';
    END $$;

    EXECUTE $$
        GRANT ALL ON SCHEMA public TO invoice_assistant_user;
    EXCEPTION WHEN duplicate_grant THEN
        RAISE NOTICE 'Grant already exists';
    END $$;

    EXECUTE $$
        ALTER SCHEMA public OWNER TO invoice_assistant_user;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Schema owner already set';
    END $$;
END $$;

DO $$
BEGIN
    -- Create hospitality database and user with idempotent checks
    EXECUTE $$
        CREATE DATABASE IF NOT EXISTS hospitality_db;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Database hospitality_db already exists';
    END $$;

    EXECUTE $$
        CREATE USER IF NOT EXISTS hospitality_user WITH PASSWORD 'change-me-secure-password';
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'User hospitality_user already exists';
    END $$;

    EXECUTE $$
        GRANT ALL PRIVILEGES ON DATABASE hospitality_db TO hospitality_user;
    EXCEPTION WHEN duplicate_privilege ON NULL THEN
        RAISE NOTICE 'Privileges already granted';
    END $$;

    EXECUTE $$
        ALTER DATABASE hospitality_db OWNER TO hospitality_user;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Owner already set';
    END $$;

    EXECUTE $$
        GRANT ALL ON SCHEMA public TO hospitality_user;
    EXCEPTION WHEN duplicate_grant THEN
        RAISE NOTICE 'Grant already exists';
    END $$;

    EXECUTE $$
        ALTER SCHEMA public OWNER TO hospitality_user;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'Schema owner already set';
    END $$;
END $$;
