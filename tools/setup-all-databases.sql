-- Fixna Ecosystem — Database Initialization Script for Dokploy / PostgreSQL
-- Run this script once as superuser (e.g. postgres) in your PostgreSQL instance.
-- Naming Convention: Database: <app>_db, Login Role: <app>_user
-- Host: fixnainfra-postgresadmin-qfasaa, Port: 5432

SELECT 'Creating databases and users and configuring permission matrix...' AS status;

DO $$
BEGIN
    -- 1. Consulting DB & User
    EXECUTE 'CREATE DATABASE consulting_db';
EXCEPTION WHEN duplicate_database THEN RAISE NOTICE 'consulting_db already exists';
END $$;

DO $$
BEGIN
    EXECUTE 'CREATE USER consulting_user WITH PASSWORD ''change-me-secure-password''';
EXCEPTION WHEN duplicate_object THEN RAISE NOTICE 'consulting_user already exists';
END $$;

ALTER DATABASE consulting_db OWNER TO consulting_user;
GRANT ALL PRIVILEGES ON DATABASE consulting_db TO consulting_user;


DO $$
BEGIN
    -- 2. LocalBoost DB & User
    EXECUTE 'CREATE DATABASE localboost_db';
EXCEPTION WHEN duplicate_database THEN RAISE NOTICE 'localboost_db already exists';
END $$;

DO $$
BEGIN
    EXECUTE 'CREATE USER localboost_user WITH PASSWORD ''change-me-secure-password''';
EXCEPTION WHEN duplicate_object THEN RAISE NOTICE 'localboost_user already exists';
END $$;

ALTER DATABASE localboost_db OWNER TO localboost_user;
GRANT ALL PRIVILEGES ON DATABASE localboost_db TO localboost_user;


DO $$
BEGIN
    -- 3. Website Audit DB & User
    EXECUTE 'CREATE DATABASE website_audit_db';
EXCEPTION WHEN duplicate_database THEN RAISE NOTICE 'website_audit_db already exists';
END $$;

DO $$
BEGIN
    EXECUTE 'CREATE USER website_audit_user WITH PASSWORD ''change-me-secure-password''';
EXCEPTION WHEN duplicate_object THEN RAISE NOTICE 'website_audit_user already exists';
END $$;

ALTER DATABASE website_audit_db OWNER TO website_audit_user;
GRANT ALL PRIVILEGES ON DATABASE website_audit_db TO website_audit_user;


DO $$
BEGIN
    -- 4. Invoice Assistant DB & User
    EXECUTE 'CREATE DATABASE invoice_assistant_db';
EXCEPTION WHEN duplicate_database THEN RAISE NOTICE 'invoice_assistant_db already exists';
END $$;

DO $$
BEGIN
    EXECUTE 'CREATE USER invoice_assistant_user WITH PASSWORD ''change-me-secure-password''';
EXCEPTION WHEN duplicate_object THEN RAISE NOTICE 'invoice_assistant_user already exists';
END $$;

ALTER DATABASE invoice_assistant_db OWNER TO invoice_assistant_user;
GRANT ALL PRIVILEGES ON DATABASE invoice_assistant_db TO invoice_assistant_user;


DO $$
BEGIN
    -- 5. Hospitality DB & User
    EXECUTE 'CREATE DATABASE hospitality_db';
EXCEPTION WHEN duplicate_database THEN RAISE NOTICE 'hospitality_db already exists';
END $$;

DO $$
BEGIN
    EXECUTE 'CREATE USER hospitality_user WITH PASSWORD ''change-me-secure-password''';
EXCEPTION WHEN duplicate_object THEN RAISE NOTICE 'hospitality_user already exists';
END $$;

ALTER DATABASE hospitality_db OWNER TO hospitality_user;
GRANT ALL PRIVILEGES ON DATABASE hospitality_db TO hospitality_user;


DO $$
BEGIN
    -- 6. AI Platform DB & User
    EXECUTE 'CREATE DATABASE ai_platform_db';
EXCEPTION WHEN duplicate_database THEN RAISE NOTICE 'ai_platform_db already exists';
END $$;

DO $$
BEGIN
    EXECUTE 'CREATE USER ai_platform_user WITH PASSWORD ''change-me-secure-password''';
EXCEPTION WHEN duplicate_object THEN RAISE NOTICE 'ai_platform_user already exists';
END $$;

ALTER DATABASE ai_platform_db OWNER TO ai_platform_user;
GRANT ALL PRIVILEGES ON DATABASE ai_platform_db TO ai_platform_user;


DO $$
BEGIN
    -- 7. Fixna User (Global Administrative / Integration Role)
    EXECUTE 'CREATE USER fixna_user WITH PASSWORD ''change-me-secure-password''';
EXCEPTION WHEN duplicate_object THEN RAISE NOTICE 'fixna_user already exists';
END $$;

ALTER DATABASE ai_platform_db OWNER TO fixna_user;
GRANT ALL PRIVILEGES ON DATABASE ai_platform_db TO fixna_user;
GRANT CONNECT ON DATABASE localboost_db TO fixna_user;


-- Grant public schema DDL create permissions to respective owners
\c consulting_db
GRANT ALL ON SCHEMA public TO consulting_user;
GRANT CREATE ON SCHEMA public TO consulting_user;

\c localboost_db
GRANT ALL ON SCHEMA public TO localboost_user;
GRANT CREATE ON SCHEMA public TO localboost_user;

\c website_audit_db
GRANT ALL ON SCHEMA public TO website_audit_user;
GRANT CREATE ON SCHEMA public TO website_audit_user;

\c invoice_assistant_db
GRANT ALL ON SCHEMA public TO invoice_assistant_user;
GRANT CREATE ON SCHEMA public TO invoice_assistant_user;

\c hospitality_db
GRANT ALL ON SCHEMA public TO hospitality_user;
GRANT CREATE ON SCHEMA public TO hospitality_user;

\c ai_platform_db
GRANT ALL ON SCHEMA public TO ai_platform_user;
GRANT CREATE ON SCHEMA public TO ai_platform_user;
GRANT ALL ON SCHEMA public TO fixna_user;
GRANT CREATE ON SCHEMA public TO fixna_user;


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
