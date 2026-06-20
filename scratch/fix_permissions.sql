-- Grant permissions on all existing tables and sequences to app user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO super_erp_app_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO super_erp_app_user;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO super_erp_app_user;

-- Make app user the owner of liquibase tracking tables (if they exist)
DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOR tbl IN
        SELECT tablename FROM pg_tables WHERE schemaname = 'public'
    LOOP
        EXECUTE 'ALTER TABLE public.' || quote_ident(tbl) || ' OWNER TO super_erp_app_user';
    END LOOP;
END $$;

-- Also set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO super_erp_app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO super_erp_app_user;
