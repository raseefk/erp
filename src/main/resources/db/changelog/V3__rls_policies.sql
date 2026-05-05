-- ============================================================
--  V3: PostgreSQL Row-Level Security (Tenant + User Level)
-- ============================================================
-- changeset system:V3-001 runOnChange:false

-- ── Helper function: get current tenant from session variable ─
CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS UUID AS $$
BEGIN
    RETURN current_setting('app.current_tenant_id', true)::UUID;
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

-- ── Helper function: get current user id from session variable ─
CREATE OR REPLACE FUNCTION current_app_user_id() RETURNS BIGINT AS $$
BEGIN
    RETURN current_setting('app.current_user_id', true)::BIGINT;
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

-- ── Enable RLS on all business tables and add policies ────────

-- app_users
ALTER TABLE app_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_users FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON app_users;
CREATE POLICY tenant_isolation ON app_users
    USING (tenant_id = current_tenant_id());

-- roles
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON roles;
CREATE POLICY tenant_isolation ON roles
    USING (tenant_id = current_tenant_id());

-- tenant_settings
ALTER TABLE tenant_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_settings FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON tenant_settings;
CREATE POLICY tenant_isolation ON tenant_settings
    USING (tenant_id = current_tenant_id());

-- tenant_feature_mapping
ALTER TABLE tenant_feature_mapping ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_feature_mapping FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON tenant_feature_mapping;
CREATE POLICY tenant_isolation ON tenant_feature_mapping
    USING (tenant_id = current_tenant_id());

-- customers
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON customers;
CREATE POLICY tenant_isolation ON customers
    USING (tenant_id = current_tenant_id());

-- inventory_items
ALTER TABLE inventory_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_items FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON inventory_items;
CREATE POLICY tenant_isolation ON inventory_items
    USING (tenant_id = current_tenant_id());

-- vendors
ALTER TABLE vendors ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendors FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON vendors;
CREATE POLICY tenant_isolation ON vendors
    USING (tenant_id = current_tenant_id());

-- employees
ALTER TABLE employees ENABLE ROW LEVEL SECURITY;
ALTER TABLE employees FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON employees;
CREATE POLICY tenant_isolation ON employees
    USING (tenant_id = current_tenant_id());

-- employee_salaries
ALTER TABLE employee_salaries ENABLE ROW LEVEL SECURITY;
ALTER TABLE employee_salaries FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON employee_salaries;
CREATE POLICY tenant_isolation ON employee_salaries
    USING (tenant_id = current_tenant_id());

-- enquiries
ALTER TABLE enquiries ENABLE ROW LEVEL SECURITY;
ALTER TABLE enquiries FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON enquiries;
CREATE POLICY tenant_isolation ON enquiries
    USING (tenant_id = current_tenant_id());

-- transactions
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON transactions;
CREATE POLICY tenant_isolation ON transactions
    USING (tenant_id = current_tenant_id());

-- transaction_items
ALTER TABLE transaction_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE transaction_items FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON transaction_items;
CREATE POLICY tenant_isolation ON transaction_items
    USING (tenant_id = current_tenant_id());

-- income_transactions
ALTER TABLE income_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE income_transactions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON income_transactions;
CREATE POLICY tenant_isolation ON income_transactions
    USING (tenant_id = current_tenant_id());

-- expenses
ALTER TABLE expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE expenses FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON expenses;
CREATE POLICY tenant_isolation ON expenses
    USING (tenant_id = current_tenant_id());

-- projects
ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE projects FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON projects;
CREATE POLICY tenant_isolation ON projects
    USING (tenant_id = current_tenant_id());

-- job_cards
ALTER TABLE job_cards ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_cards FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON job_cards;
CREATE POLICY tenant_isolation ON job_cards
    USING (tenant_id = current_tenant_id());

-- project_expenses
ALTER TABLE project_expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_expenses FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON project_expenses;
CREATE POLICY tenant_isolation ON project_expenses
    USING (tenant_id = current_tenant_id());

-- project_labour
ALTER TABLE project_labour ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_labour FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON project_labour;
CREATE POLICY tenant_isolation ON project_labour
    USING (tenant_id = current_tenant_id());

-- daily_logs
ALTER TABLE daily_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_logs FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON daily_logs;
CREATE POLICY tenant_isolation ON daily_logs
    USING (tenant_id = current_tenant_id());

-- daily_labour_logs
ALTER TABLE daily_labour_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_labour_logs FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON daily_labour_logs;
CREATE POLICY tenant_isolation ON daily_labour_logs
    USING (tenant_id = current_tenant_id());

-- attendance
ALTER TABLE attendance ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON attendance;
CREATE POLICY tenant_isolation ON attendance
    USING (tenant_id = current_tenant_id());

-- leave_balances
ALTER TABLE leave_balances ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave_balances FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON leave_balances;
CREATE POLICY tenant_isolation ON leave_balances
    USING (tenant_id = current_tenant_id());

-- leave_applications
ALTER TABLE leave_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave_applications FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON leave_applications;
CREATE POLICY tenant_isolation ON leave_applications
    USING (tenant_id = current_tenant_id());

-- holidays
ALTER TABLE holidays ENABLE ROW LEVEL SECURITY;
ALTER TABLE holidays FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON holidays;
CREATE POLICY tenant_isolation ON holidays
    USING (tenant_id = current_tenant_id());

-- purchase_orders
ALTER TABLE purchase_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE purchase_orders FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON purchase_orders;
CREATE POLICY tenant_isolation ON purchase_orders
    USING (tenant_id = current_tenant_id());

-- purchase_order_items
ALTER TABLE purchase_order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE purchase_order_items FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON purchase_order_items;
CREATE POLICY tenant_isolation ON purchase_order_items
    USING (tenant_id = current_tenant_id());

-- company_settings
ALTER TABLE company_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE company_settings FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON company_settings;
CREATE POLICY tenant_isolation ON company_settings
    USING (tenant_id = current_tenant_id());

-- audit_log (tenant-scoped reads, but all inserts allowed for the app user)
ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_read ON audit_log;
CREATE POLICY tenant_isolation_read ON audit_log
    FOR SELECT USING (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS audit_insert ON audit_log;
CREATE POLICY audit_insert ON audit_log
    FOR INSERT WITH CHECK (true);
