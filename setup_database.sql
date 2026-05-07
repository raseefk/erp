-- =============================================================================
-- PostgreSQL Database Setup Script for Super ERP (Multi-Tenant)
-- =============================================================================
-- This script prepares the environment for the application to run with 
-- PostgreSQL Row Level Security (RLS).
-- Execute these commands as a superuser (e.g., 'postgres').

-- 1. Create the Database
CREATE DATABASE super_erp;

-- 2. Create the Application User
-- Replace 'StrongPassword123!' with a secure password.
CREATE USER erp_user WITH PASSWORD 'StrongPassword123!';

-- 3. Connect to the new database before running the next commands
-- \c super_erp

-- 4. Enable Required Extensions
-- These are used for UUID generation and password hashing (if done in DB).
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 5. Grant Schema Permissions
-- The user needs to be able to create tables and manage the schema (Liquibase).
GRANT ALL PRIVILEGES ON DATABASE super_erp TO erp_user;
GRANT ALL ON SCHEMA public TO erp_user;

-- 6. Important: RLS Policy Permissions
-- In PostgreSQL, only the table owner or a superuser can create/modify RLS policies.
-- Since the application user (erp_user) will run Liquibase to create tables,
-- it will automatically become the OWNER of those tables and can manage policies.
-- No additional 'GRANT' is needed for policies if the user owns the table.

-- 7. Ensure the user can use the extensions
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO erp_user;

-- =============================================================================
-- SERVER & FILE SYSTEM SETUP
-- =============================================================================
-- 1. Upload Directory Permissions
-- The application stores files in './uploads' (or the path defined in 
-- app.upload.dir). Ensure the OS user running the application has READ/WRITE
-- permissions to this directory.
--
-- Example (Linux):
-- mkdir -p /opt/super-erp/uploads
-- chown -R erp-user:erp-user /opt/super-erp/uploads
-- chmod -R 755 /opt/super-erp/uploads

-- 2. PostgreSQL Session Variables (RLS)
-- The application uses custom session variables (app.current_tenant_id, etc.)
-- for Row Level Security. By default, PostgreSQL allows any custom variable 
-- prefix. If you encounter errors, ensure the 'custom_variable_classes' (for 
-- very old Postgres versions) is not restricting these.

-- 3. Timezones
-- The application uses OffsetDateTime (TIMESTAMPTZ). It is highly recommended 
-- to set the database and server timezones to UTC for consistency.
-- ALTER DATABASE super_erp SET timezone TO 'UTC';

-- =============================================================================
-- APPLICATION CONFIGURATION (application.properties / environment variables)
-- =============================================================================
-- spring.datasource.url=jdbc:postgresql://<your-db-host>:5432/super_erp
-- spring.datasource.username=erp_user
-- spring.datasource.password=StrongPassword123!
-- spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
-- spring.liquibase.enabled=true
-- app.upload.dir=/opt/super-erp/uploads
-- =============================================================================
