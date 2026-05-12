--liquibase formatted sql

--changeset supererp:V18-001-asset-foundation dbms:postgresql
CREATE TABLE IF NOT EXISTS assets (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    asset_code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(100),
    description TEXT,
    serial_number VARCHAR(100),
    purchase_date DATE,
    purchase_cost NUMERIC(14,2),
    salvage_value NUMERIC(14,2),
    useful_life_months INTEGER,
    depreciation_method VARCHAR(20) NOT NULL,
    depreciation_rate_percent NUMERIC(7,4),
    current_book_value NUMERIC(14,2),
    status VARCHAR(30) NOT NULL,
    vendor_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_assets_tenant_code UNIQUE (tenant_id, asset_code),
    CONSTRAINT fk_assets_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id)
);

CREATE TABLE IF NOT EXISTS asset_depreciation_schedules (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    asset_id BIGINT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    opening_value NUMERIC(14,2) NOT NULL,
    depreciation_amount NUMERIC(14,2) NOT NULL,
    closing_value NUMERIC(14,2) NOT NULL,
    method VARCHAR(20) NOT NULL,
    posted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    CONSTRAINT fk_asset_dep_asset FOREIGN KEY (asset_id) REFERENCES assets(id)
);

CREATE TABLE IF NOT EXISTS asset_assignments (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    asset_id BIGINT NOT NULL,
    assignment_type VARCHAR(20) NOT NULL,
    employee_id BIGINT,
    project_id BIGINT,
    location VARCHAR(300),
    assigned_from DATE NOT NULL,
    assigned_to DATE,
    returned_at DATE,
    notes TEXT,
    created_at TIMESTAMP,
    CONSTRAINT fk_asset_assign_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
    CONSTRAINT fk_asset_assign_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_asset_assign_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE IF NOT EXISTS preventive_maintenance_plans (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    asset_id BIGINT NOT NULL,
    frequency VARCHAR(30) NOT NULL,
    custom_interval_days INTEGER,
    next_due_date DATE NOT NULL,
    assigned_employee_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    instructions TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_pm_plan_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
    CONSTRAINT fk_pm_plan_employee FOREIGN KEY (assigned_employee_id) REFERENCES employees(id)
);

CREATE TABLE IF NOT EXISTS asset_maintenance_jobs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    asset_id BIGINT NOT NULL,
    maintenance_plan_id BIGINT,
    job_card_id BIGINT,
    scheduled_date DATE NOT NULL,
    completed_date DATE,
    status VARCHAR(30) NOT NULL,
    assigned_employee_id BIGINT,
    cost NUMERIC(14,2),
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_asset_maint_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
    CONSTRAINT fk_asset_maint_plan FOREIGN KEY (maintenance_plan_id) REFERENCES preventive_maintenance_plans(id),
    CONSTRAINT fk_asset_maint_job_card FOREIGN KEY (job_card_id) REFERENCES job_cards(id),
    CONSTRAINT fk_asset_maint_employee FOREIGN KEY (assigned_employee_id) REFERENCES employees(id)
);

CREATE TABLE IF NOT EXISTS asset_breakdowns (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    asset_id BIGINT NOT NULL,
    reported_at TIMESTAMP NOT NULL,
    repaired_at TIMESTAMP,
    downtime_minutes BIGINT,
    reported_by_id BIGINT,
    assigned_employee_id BIGINT,
    symptom VARCHAR(500) NOT NULL,
    root_cause TEXT,
    repair_action TEXT,
    repair_cost NUMERIC(14,2),
    status VARCHAR(20) NOT NULL,
    job_card_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_asset_breakdown_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
    CONSTRAINT fk_asset_breakdown_reported_by FOREIGN KEY (reported_by_id) REFERENCES app_users(id),
    CONSTRAINT fk_asset_breakdown_employee FOREIGN KEY (assigned_employee_id) REFERENCES employees(id),
    CONSTRAINT fk_asset_breakdown_job_card FOREIGN KEY (job_card_id) REFERENCES job_cards(id)
);

CREATE INDEX IF NOT EXISTS idx_assets_tenant_status ON assets(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_asset_dep_asset_period ON asset_depreciation_schedules(asset_id, period_start);
CREATE INDEX IF NOT EXISTS idx_asset_assign_asset_active ON asset_assignments(asset_id, returned_at);
CREATE INDEX IF NOT EXISTS idx_pm_plan_due ON preventive_maintenance_plans(tenant_id, active, next_due_date);
CREATE INDEX IF NOT EXISTS idx_asset_maint_scheduled ON asset_maintenance_jobs(tenant_id, scheduled_date);
CREATE INDEX IF NOT EXISTS idx_asset_breakdown_asset ON asset_breakdowns(asset_id, reported_at DESC);

--changeset supererp:V18-002-construction-foundation dbms:postgresql
CREATE TABLE IF NOT EXISTS bill_of_quantities (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    project_id BIGINT NOT NULL,
    boq_number VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    approved_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_boq_tenant_number UNIQUE (tenant_id, boq_number),
    CONSTRAINT fk_boq_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE IF NOT EXISTS boq_items (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    boq_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    inventory_item_id BIGINT,
    description VARCHAR(500) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    quantity NUMERIC(14,3) NOT NULL,
    rate NUMERIC(14,2) NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    completed_quantity NUMERIC(14,3) NOT NULL,
    completion_percent NUMERIC(7,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    linked_job_card_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_boq_item_boq FOREIGN KEY (boq_id) REFERENCES bill_of_quantities(id),
    CONSTRAINT fk_boq_item_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_boq_item_inventory FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_boq_item_job_card FOREIGN KEY (linked_job_card_id) REFERENCES job_cards(id)
);

CREATE TABLE IF NOT EXISTS boq_progress_entries (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    boq_item_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    job_card_id BIGINT,
    progress_date DATE NOT NULL,
    completed_quantity NUMERIC(14,3) NOT NULL,
    remarks TEXT,
    recorded_by_id BIGINT,
    created_at TIMESTAMP,
    CONSTRAINT fk_boq_progress_item FOREIGN KEY (boq_item_id) REFERENCES boq_items(id),
    CONSTRAINT fk_boq_progress_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_boq_progress_job_card FOREIGN KEY (job_card_id) REFERENCES job_cards(id),
    CONSTRAINT fk_boq_progress_user FOREIGN KEY (recorded_by_id) REFERENCES app_users(id)
);

CREATE TABLE IF NOT EXISTS subcontractor_running_bills (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    project_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    bill_number VARCHAR(50) NOT NULL,
    bill_date DATE NOT NULL,
    period_from DATE,
    period_to DATE,
    gross_amount NUMERIC(14,2) NOT NULL,
    deduction_amount NUMERIC(14,2) NOT NULL,
    certified_amount NUMERIC(14,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    submitted_by_id BIGINT,
    certified_by_id BIGINT,
    certified_at TIMESTAMP,
    rejection_reason TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_sub_bill_tenant_number UNIQUE (tenant_id, bill_number),
    CONSTRAINT fk_sub_bill_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_sub_bill_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    CONSTRAINT fk_sub_bill_submitted_by FOREIGN KEY (submitted_by_id) REFERENCES app_users(id),
    CONSTRAINT fk_sub_bill_certified_by FOREIGN KEY (certified_by_id) REFERENCES app_users(id)
);

CREATE TABLE IF NOT EXISTS subcontractor_running_bill_items (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    running_bill_id BIGINT NOT NULL,
    boq_item_id BIGINT,
    description VARCHAR(500) NOT NULL,
    claimed_quantity NUMERIC(14,3) NOT NULL,
    certified_quantity NUMERIC(14,3) NOT NULL,
    rate NUMERIC(14,2) NOT NULL,
    claimed_amount NUMERIC(14,2) NOT NULL,
    certified_amount NUMERIC(14,2) NOT NULL,
    remarks TEXT,
    CONSTRAINT fk_sub_bill_item_bill FOREIGN KEY (running_bill_id) REFERENCES subcontractor_running_bills(id),
    CONSTRAINT fk_sub_bill_item_boq_item FOREIGN KEY (boq_item_id) REFERENCES boq_items(id)
);

CREATE TABLE IF NOT EXISTS material_site_transactions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    project_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    transaction_date DATE NOT NULL,
    quantity NUMERIC(14,3) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    reference_type VARCHAR(100),
    reference_id VARCHAR(100),
    remarks TEXT,
    recorded_by_id BIGINT,
    created_at TIMESTAMP,
    CONSTRAINT fk_mat_site_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_mat_site_inventory FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_mat_site_user FOREIGN KEY (recorded_by_id) REFERENCES app_users(id)
);

CREATE TABLE IF NOT EXISTS project_milestones (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    project_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    due_date DATE,
    contract_amount NUMERIC(14,2) NOT NULL,
    release_percent NUMERIC(7,2) NOT NULL,
    release_amount NUMERIC(14,2) NOT NULL,
    status VARCHAR(40) NOT NULL,
    submitted_at TIMESTAMP,
    client_approved_at TIMESTAMP,
    payment_released_at TIMESTAMP,
    client_approval_reference VARCHAR(200),
    linked_transaction_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_project_milestone_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_project_milestone_transaction FOREIGN KEY (linked_transaction_id) REFERENCES transactions(id)
);

CREATE INDEX IF NOT EXISTS idx_boq_project ON bill_of_quantities(tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_boq_item_project_status ON boq_items(tenant_id, project_id, status);
CREATE INDEX IF NOT EXISTS idx_boq_progress_project_date ON boq_progress_entries(tenant_id, project_id, progress_date DESC);
CREATE INDEX IF NOT EXISTS idx_sub_bill_project_status ON subcontractor_running_bills(tenant_id, project_id, status);
CREATE INDEX IF NOT EXISTS idx_material_site_project_item ON material_site_transactions(tenant_id, project_id, inventory_item_id);
CREATE INDEX IF NOT EXISTS idx_project_milestone_status ON project_milestones(tenant_id, project_id, status);

--changeset supererp:V18-003-rls-policies dbms:postgresql
ALTER TABLE assets ENABLE ROW LEVEL SECURITY;
ALTER TABLE assets FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON assets;
CREATE POLICY tenant_isolation ON assets USING (is_system_admin() OR tenant_id = current_tenant_id());

ALTER TABLE asset_depreciation_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE asset_depreciation_schedules FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON asset_depreciation_schedules;
CREATE POLICY tenant_isolation ON asset_depreciation_schedules USING (is_system_admin() OR tenant_id = current_tenant_id());

ALTER TABLE asset_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE asset_assignments FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON asset_assignments;
CREATE POLICY tenant_isolation ON asset_assignments USING (is_system_admin() OR tenant_id = current_tenant_id());

ALTER TABLE preventive_maintenance_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE preventive_maintenance_plans FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON preventive_maintenance_plans;
CREATE POLICY tenant_isolation ON preventive_maintenance_plans USING (is_system_admin() OR tenant_id = current_tenant_id());

ALTER TABLE asset_maintenance_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE asset_maintenance_jobs FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON asset_maintenance_jobs;
CREATE POLICY tenant_isolation ON asset_maintenance_jobs USING (is_system_admin() OR tenant_id = current_tenant_id());

ALTER TABLE asset_breakdowns ENABLE ROW LEVEL SECURITY;
ALTER TABLE asset_breakdowns FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON asset_breakdowns;
CREATE POLICY tenant_isolation ON asset_breakdowns USING (is_system_admin() OR tenant_id = current_tenant_id());

ALTER TABLE bill_of_quantities ENABLE ROW LEVEL SECURITY;
ALTER TABLE bill_of_quantities FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON bill_of_quantities;
CREATE POLICY tenant_isolation ON bill_of_quantities USING (is_system_admin() OR tenant_id = current_tenant_id());

ALTER TABLE boq_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE boq_items FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON boq_items;
CREATE POLICY tenant_isolation ON boq_items USING (is_system_admin() OR tenant_id = current_tenant_id());

ALTER TABLE boq_progress_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE boq_progress_entries FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON boq_progress_entries;
CREATE POLICY tenant_isolation ON boq_progress_entries USING (is_system_admin() OR tenant_id = current_tenant_id());

ALTER TABLE subcontractor_running_bills ENABLE ROW LEVEL SECURITY;
ALTER TABLE subcontractor_running_bills FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON subcontractor_running_bills;
CREATE POLICY tenant_isolation ON subcontractor_running_bills USING (is_system_admin() OR tenant_id = current_tenant_id());

ALTER TABLE subcontractor_running_bill_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE subcontractor_running_bill_items FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON subcontractor_running_bill_items;
CREATE POLICY tenant_isolation ON subcontractor_running_bill_items USING (is_system_admin() OR tenant_id = current_tenant_id());

ALTER TABLE material_site_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE material_site_transactions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON material_site_transactions;
CREATE POLICY tenant_isolation ON material_site_transactions USING (is_system_admin() OR tenant_id = current_tenant_id());

ALTER TABLE project_milestones ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_milestones FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON project_milestones;
CREATE POLICY tenant_isolation ON project_milestones USING (is_system_admin() OR tenant_id = current_tenant_id());
