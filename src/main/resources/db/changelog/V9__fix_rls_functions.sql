-- V9: Fix RLS functions to handle empty strings and allow System Admin bypass

-- 1. Update current_tenant_id helper
CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS UUID AS $$
DECLARE
    val TEXT;
BEGIN
    val := current_setting('app.current_tenant_id', true);
    IF val IS NULL OR val = '' THEN
        RETURN NULL;
    END IF;
    RETURN val::UUID;
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

-- 2. Helper to check if current user is System Admin
CREATE OR REPLACE FUNCTION is_system_admin() RETURNS BOOLEAN AS $$
BEGIN
    RETURN current_setting('app.is_system_admin', true) = 'true';
EXCEPTION WHEN OTHERS THEN
    RETURN FALSE;
END;
$$ LANGUAGE plpgsql STABLE;

-- 3. Update all existing policies to allow System Admin bypass
-- Note: Re-creating policies with the bypass logic

-- Roles
DROP POLICY IF EXISTS tenant_isolation ON roles;
CREATE POLICY tenant_isolation ON roles
    USING (tenant_id = current_tenant_id() OR is_system_admin());

-- Users
DROP POLICY IF EXISTS tenant_isolation ON app_users;
CREATE POLICY tenant_isolation ON app_users
    USING (tenant_id = current_tenant_id() OR is_system_admin());

-- Feature Mapping
DROP POLICY IF EXISTS tenant_isolation ON tenant_feature_mapping;
CREATE POLICY tenant_isolation ON tenant_feature_mapping
    USING (tenant_id = current_tenant_id() OR is_system_admin());

-- Menu Mapping (Fixed the V7 crash here)
DROP POLICY IF EXISTS tenant_isolation ON tenant_menu_mapping;
CREATE POLICY tenant_isolation ON tenant_menu_mapping
    USING (tenant_id = current_tenant_id() OR is_system_admin());

-- Company Settings
DROP POLICY IF EXISTS tenant_isolation ON company_settings;
CREATE POLICY tenant_isolation ON company_settings
    USING (tenant_id = current_tenant_id() OR is_system_admin());

-- Audit Logs (Always visible to system admin for forensic analysis)
DROP POLICY IF EXISTS tenant_isolation ON audit_log;
CREATE POLICY tenant_isolation ON audit_log
    USING (tenant_id = current_tenant_id() OR is_system_admin());

-- Core Business Entities
DROP POLICY IF EXISTS tenant_isolation ON customers;
CREATE POLICY tenant_isolation ON customers USING (tenant_id = current_tenant_id() OR is_system_admin());

DROP POLICY IF EXISTS tenant_isolation ON inventory_items;
CREATE POLICY tenant_isolation ON inventory_items USING (tenant_id = current_tenant_id() OR is_system_admin());

DROP POLICY IF EXISTS tenant_isolation ON vendors;
CREATE POLICY tenant_isolation ON vendors USING (tenant_id = current_tenant_id() OR is_system_admin());

DROP POLICY IF EXISTS tenant_isolation ON employees;
CREATE POLICY tenant_isolation ON employees USING (tenant_id = current_tenant_id() OR is_system_admin());

DROP POLICY IF EXISTS tenant_isolation ON enquiries;
CREATE POLICY tenant_isolation ON enquiries USING (tenant_id = current_tenant_id() OR is_system_admin());

DROP POLICY IF EXISTS tenant_isolation ON transactions;
CREATE POLICY tenant_isolation ON transactions USING (tenant_id = current_tenant_id() OR is_system_admin());

DROP POLICY IF EXISTS tenant_isolation ON projects;
CREATE POLICY tenant_isolation ON projects USING (tenant_id = current_tenant_id() OR is_system_admin());
