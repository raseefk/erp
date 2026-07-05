GRANT ALL PRIVILEGES ON DATABASE super_erp TO super_erp_app_user;
GRANT USAGE ON SCHEMA public TO super_erp_app_user;
GRANT ALL ON SCHEMA public TO super_erp_app_user;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO super_erp_app_user;
ALTER DATABASE super_erp SET timezone TO 'UTC';
