-- V10: Fix Audit Log RLS and ensure System Admin bypass works for all tables
-- changeset system:V10-001 runOnChange:false

-- 1. Fix Audit Log Policy (Explicitly allow System Admin and handle NULL tenant_id)
DROP POLICY IF EXISTS tenant_isolation ON audit_log;
DROP POLICY IF EXISTS tenant_isolation_read ON audit_log;
DROP POLICY IF EXISTS audit_insert ON audit_log;

CREATE POLICY audit_policy ON audit_log
    USING (is_system_admin() OR tenant_id = current_tenant_id() OR (tenant_id IS NULL AND is_system_admin()))
    WITH CHECK (true);

-- 2. Ensure all other tables have the system admin bypass correctly applied
-- We use a more robust check in the USING clause

DROP POLICY IF EXISTS tenant_isolation ON roles;
CREATE POLICY tenant_isolation ON roles USING (is_system_admin() OR tenant_id = current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation ON app_users;
CREATE POLICY tenant_isolation ON app_users USING (is_system_admin() OR tenant_id = current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation ON tenant_feature_mapping;
CREATE POLICY tenant_isolation ON tenant_feature_mapping USING (is_system_admin() OR tenant_id = current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation ON tenant_menu_mapping;
CREATE POLICY tenant_isolation ON tenant_menu_mapping USING (is_system_admin() OR tenant_id = current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation ON company_settings;
CREATE POLICY tenant_isolation ON company_settings USING (is_system_admin() OR tenant_id = current_tenant_id());

-- 3. Grant permissions to erp_app just in case ownership was lost during migration
GRANT ALL ON audit_log TO erp_app;
