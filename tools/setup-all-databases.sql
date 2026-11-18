-- Fixna Ecosystem — Database Initialization Script for Dokploy / PostgreSQL
-- Run this script once as superuser (e.g. postgres) in your PostgreSQL instance.
-- Naming Convention: Database: <app>_db, Login Role: <app>_user
-- Host: fixnainfra-postgresadmin-qfasaa, Port: 5432

SELECT 'Creating databases and users...' AS status;

CREATE DATABASE consulting_db;
CREATE USER consulting_user WITH PASSWORD 'change-me-secure-password';
GRANT ALL PRIVILEGES ON DATABASE consulting_db TO consulting_user;

CREATE DATABASE localboost_db;
CREATE USER localboost_user WITH PASSWORD 'change-me-secure-password';
GRANT ALL PRIVILEGES ON DATABASE localboost_db TO localboost_user;

CREATE DATABASE website_audit_db;
CREATE USER website_audit_user WITH PASSWORD 'change-me-secure-password';
GRANT ALL PRIVILEGES ON DATABASE website_audit_db TO website_audit_user;

CREATE DATABASE invoice_assistant_db;
CREATE USER invoice_assistant_user WITH PASSWORD 'change-me-secure-password';
GRANT ALL PRIVILEGES ON DATABASE invoice_assistant_db TO invoice_assistant_user;

CREATE DATABASE hospitality_db;
CREATE USER hospitality_user WITH PASSWORD 'change-me-secure-password';
GRANT ALL PRIVILEGES ON DATABASE hospitality_db TO hospitality_user;



