-- V7: Enable RLS on new tenant_menu_mapping table
ALTER TABLE tenant_menu_mapping ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_menu_mapping FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON tenant_menu_mapping;
CREATE POLICY tenant_isolation ON tenant_menu_mapping
    USING (tenant_id = current_setting('app.current_tenant_id', true)::UUID);
