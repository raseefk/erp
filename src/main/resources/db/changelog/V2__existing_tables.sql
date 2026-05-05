-- ============================================================
--  V2: Existing Business Tables (tenant-aware)
-- ============================================================

-- changeset system:V2-001 runOnChange:false

-- ── CUSTOMERS ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customers (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(255) NOT NULL,
    phone           VARCHAR(30),
    email           VARCHAR(255),
    address         TEXT,
    gst_number      VARCHAR(20),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── INVENTORY ITEMS ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS inventory_items (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(255) NOT NULL,
    item_type       VARCHAR(20) NOT NULL,
    description     TEXT,
    current_price   NUMERIC(14,2) NOT NULL DEFAULT 0,
    stock_quantity  INT NOT NULL DEFAULT 0,
    hsn_sac_code    VARCHAR(20),
    unit            VARCHAR(20),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── VENDORS ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vendors (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(255) NOT NULL,
    contact_person  VARCHAR(255),
    phone           VARCHAR(30),
    email           VARCHAR(255),
    address         TEXT,
    gst_number      VARCHAR(20),
    material_supplied TEXT,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── EMPLOYEES ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS employees (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(255) NOT NULL,
    phone           VARCHAR(30),
    email           VARCHAR(255),
    designation     VARCHAR(255),
    department      VARCHAR(255),
    monthly_salary  NUMERIC(12,2),
    joining_date    DATE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── EMPLOYEE SALARIES ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS employee_salaries (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    employee_id     BIGINT NOT NULL REFERENCES employees(id),
    amount          NUMERIC(12,2) NOT NULL,
    pay_date        DATE NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── ENQUIRIES ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS enquiries (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(255) NOT NULL,
    phone           VARCHAR(30),
    email           VARCHAR(255),
    service         VARCHAR(255),
    message         TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'NEW',
    admin_notes     TEXT,
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── TRANSACTIONS (Billing / Quotations) ──────────────────────
CREATE TABLE IF NOT EXISTS transactions (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    transaction_number VARCHAR(30),
    transaction_type VARCHAR(30) NOT NULL,
    customer_id     BIGINT REFERENCES customers(id),
    customer_name   VARCHAR(255),
    customer_phone  VARCHAR(30),
    customer_email  VARCHAR(255),
    customer_address TEXT,
    customer_gst    VARCHAR(20),
    gst_enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    gst_type        VARCHAR(20) DEFAULT 'LOCAL',
    tax_all_items   BOOLEAN NOT NULL DEFAULT FALSE,
    cgst_rate       NUMERIC(5,2) DEFAULT 9,
    sgst_rate       NUMERIC(5,2) DEFAULT 9,
    igst_rate       NUMERIC(5,2) DEFAULT 18,
    subtotal        NUMERIC(14,2) NOT NULL DEFAULT 0,
    cgst_amount     NUMERIC(14,2) NOT NULL DEFAULT 0,
    sgst_amount     NUMERIC(14,2) NOT NULL DEFAULT 0,
    igst_amount     NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_amount    NUMERIC(14,2) NOT NULL DEFAULT 0,
    payment_status  VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    paid_amount     NUMERIC(14,2) NOT NULL DEFAULT 0,
    notes           TEXT,
    converted_from_id BIGINT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── TRANSACTION ITEMS ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transaction_items (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    transaction_id  BIGINT NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    inventory_item_id BIGINT REFERENCES inventory_items(id),
    description     VARCHAR(500) NOT NULL,
    item_type       VARCHAR(20),
    hsn_sac_code    VARCHAR(20),
    unit            VARCHAR(20),
    quantity        NUMERIC(12,3) NOT NULL DEFAULT 1,
    unit_price      NUMERIC(14,2) NOT NULL,
    line_total      NUMERIC(14,2) NOT NULL,
    taxable         BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order      INT DEFAULT 0
);

-- ── INCOME TRANSACTIONS ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS income_transactions (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    transaction_id  BIGINT REFERENCES transactions(id),
    amount          NUMERIC(14,2) NOT NULL,
    payment_mode    VARCHAR(50),
    reference       VARCHAR(255),
    notes           TEXT,
    payment_date    DATE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── EXPENSES ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS expenses (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    category        VARCHAR(50) NOT NULL,
    description     VARCHAR(500) NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    expense_date    DATE NOT NULL,
    vendor_id       BIGINT REFERENCES vendors(id),
    employee_id     BIGINT REFERENCES employees(id),
    reference_id    VARCHAR(100),
    notes           TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    approved_by     BIGINT REFERENCES app_users(id),
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── PROJECTS ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS projects (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(255) NOT NULL,
    client_name     VARCHAR(255),
    location        VARCHAR(500),
    description     TEXT,
    total_contract_value NUMERIC(14,2),
    start_date      DATE,
    end_date        DATE,
    status          VARCHAR(30) NOT NULL DEFAULT 'PLANNING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── JOB CARDS ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS job_cards (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    project_id      BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    phase           VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    target_date     DATE,
    completed_date  DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── PROJECT EXPENSES ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS project_expenses (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    project_id      BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    category        VARCHAR(50) NOT NULL,
    description     VARCHAR(500) NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    vendor_id       BIGINT REFERENCES vendors(id),
    expense_date    DATE NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    approved_by     BIGINT REFERENCES app_users(id),
    approved_at     TIMESTAMPTZ,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── PROJECT LABOUR ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS project_labour (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    project_id      BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    employee_id     BIGINT REFERENCES employees(id),
    worker_name     VARCHAR(255),
    work_date       DATE NOT NULL,
    hours_worked    NUMERIC(5,2),
    daily_wage      NUMERIC(10,2),
    total_amount    NUMERIC(12,2),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── DAILY LOG ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS daily_logs (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    project_id      BIGINT REFERENCES projects(id),
    job_card_id     BIGINT REFERENCES job_cards(id),
    log_date        DATE NOT NULL,
    work_summary    TEXT NOT NULL,
    issues          TEXT,
    weather         VARCHAR(50),
    supervisor_name VARCHAR(255),
    created_by      BIGINT REFERENCES app_users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── DAILY LABOUR LOG ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS daily_labour_logs (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    daily_log_id    BIGINT NOT NULL REFERENCES daily_logs(id) ON DELETE CASCADE,
    worker_name     VARCHAR(255) NOT NULL,
    hours_worked    NUMERIC(5,2),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── ATTENDANCE ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS attendance (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    employee_id     BIGINT NOT NULL REFERENCES employees(id),
    attendance_date DATE NOT NULL,
    status          VARCHAR(30) NOT NULL,
    check_in        TIME,
    check_out       TIME,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, employee_id, attendance_date)
);

-- ── LEAVE BALANCE ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS leave_balances (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    employee_id     BIGINT NOT NULL REFERENCES employees(id),
    year            INT NOT NULL,
    leave_type      VARCHAR(30) NOT NULL,
    total_days      INT NOT NULL DEFAULT 0,
    used_days       INT NOT NULL DEFAULT 0,
    UNIQUE(tenant_id, employee_id, year, leave_type)
);

-- ── LEAVE APPLICATIONS ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS leave_applications (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    employee_id     BIGINT NOT NULL REFERENCES employees(id),
    leave_type      VARCHAR(30) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    days_count      INT NOT NULL,
    reason          TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    approved_by     BIGINT REFERENCES app_users(id),
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── HOLIDAYS ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS holidays (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(255) NOT NULL,
    holiday_date    DATE NOT NULL,
    UNIQUE(tenant_id, holiday_date)
);

-- ── PURCHASE ORDERS ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS purchase_orders (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    po_number       VARCHAR(30),
    vendor_id       BIGINT NOT NULL REFERENCES vendors(id),
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    order_date      DATE NOT NULL,
    expected_date   DATE,
    total_amount    NUMERIC(14,2) NOT NULL DEFAULT 0,
    notes           TEXT,
    received_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── PURCHASE ORDER ITEMS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS purchase_order_items (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    inventory_item_id BIGINT REFERENCES inventory_items(id),
    description     VARCHAR(500) NOT NULL,
    quantity        NUMERIC(12,3) NOT NULL,
    unit_price      NUMERIC(14,2) NOT NULL,
    total_price     NUMERIC(14,2) NOT NULL
);

-- ── COMPANY SETTINGS ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS company_settings (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) UNIQUE,
    company_name    VARCHAR(255),
    tagline         VARCHAR(500),
    address         TEXT,
    phone           VARCHAR(50),
    email           VARCHAR(255),
    website         VARCHAR(255),
    gst_number      VARCHAR(30),
    cgst_rate       NUMERIC(5,2) DEFAULT 9,
    sgst_rate       NUMERIC(5,2) DEFAULT 9,
    logo_path       TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── INDEXES ON ALL TENANT COLUMNS ────────────────────────────
CREATE INDEX IF NOT EXISTS idx_customers_tenant      ON customers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_inventory_tenant      ON inventory_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_vendors_tenant        ON vendors(tenant_id);
CREATE INDEX IF NOT EXISTS idx_employees_tenant      ON employees(tenant_id);
CREATE INDEX IF NOT EXISTS idx_emp_sal_tenant        ON employee_salaries(tenant_id);
CREATE INDEX IF NOT EXISTS idx_enquiries_tenant      ON enquiries(tenant_id);
CREATE INDEX IF NOT EXISTS idx_transactions_tenant   ON transactions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_txn_items_tenant      ON transaction_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_income_txn_tenant     ON income_transactions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_expenses_tenant       ON expenses(tenant_id);
CREATE INDEX IF NOT EXISTS idx_projects_tenant       ON projects(tenant_id);
CREATE INDEX IF NOT EXISTS idx_job_cards_tenant      ON job_cards(tenant_id);
CREATE INDEX IF NOT EXISTS idx_proj_exp_tenant       ON project_expenses(tenant_id);
CREATE INDEX IF NOT EXISTS idx_proj_lab_tenant       ON project_labour(tenant_id);
CREATE INDEX IF NOT EXISTS idx_daily_log_tenant      ON daily_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_daily_lab_tenant      ON daily_labour_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_attendance_tenant     ON attendance(tenant_id);
CREATE INDEX IF NOT EXISTS idx_leave_bal_tenant      ON leave_balances(tenant_id);
CREATE INDEX IF NOT EXISTS idx_leave_app_tenant      ON leave_applications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_holidays_tenant       ON holidays(tenant_id);
CREATE INDEX IF NOT EXISTS idx_po_tenant             ON purchase_orders(tenant_id);
CREATE INDEX IF NOT EXISTS idx_po_items_tenant       ON purchase_order_items(tenant_id);
