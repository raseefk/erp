--liquibase formatted sql
-- ============================================================
--  V2: Existing Business Tables (tenant-aware)
-- ============================================================

-- changeset system:V2-001-v4 runOnChange:false

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
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── VENDORS ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vendors (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(200) NOT NULL,
    contact_person  VARCHAR(200),
    phone           VARCHAR(30),
    email           VARCHAR(200),
    address         TEXT,
    gst_number      VARCHAR(20),
    material_supplied VARCHAR(200),
    bank_name       VARCHAR(100),
    bank_account_number VARCHAR(50),
    ifsc_code       VARCHAR(20),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── EMPLOYEES ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS employees (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    app_user_id     BIGINT REFERENCES app_users(id),
    employee_code   VARCHAR(50) UNIQUE,
    name            VARCHAR(150) NOT NULL,
    phone           VARCHAR(30),
    email           VARCHAR(200),
    designation     VARCHAR(100),
    department      VARCHAR(100),
    monthly_salary  NUMERIC(12,2) DEFAULT 0,
    joining_date    DATE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ── EXPENSES ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS expenses (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            UUID NOT NULL REFERENCES tenants(id),
    category             VARCHAR(50) NOT NULL,
    description          VARCHAR(300) NOT NULL,
    amount               NUMERIC(12,2) NOT NULL,
    expense_date         DATE NOT NULL,
    reference            VARCHAR(200),
    employee_id          BIGINT REFERENCES employees(id),
    attachment_name      VARCHAR(255),
    attachment_path      VARCHAR(500),
    attachment_mime_type VARCHAR(100),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── EMPLOYEE SALARY ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS employee_salaries (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             UUID NOT NULL REFERENCES tenants(id),
    employee_id           BIGINT NOT NULL REFERENCES employees(id),
    salary_month_year     VARCHAR(20) NOT NULL,
    amount                NUMERIC(12,2) NOT NULL,
    salary_credited_date  DATE NOT NULL,
    expense_id            BIGINT REFERENCES expenses(id),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_emp_salary_month UNIQUE(tenant_id, employee_id, salary_month_year)
);

-- ── ENQUIRIES ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS enquiries (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(150) NOT NULL,
    phone           VARCHAR(15) NOT NULL,
    email           VARCHAR(200),
    service         VARCHAR(100),
    message         TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'NEW',
    admin_notes     TEXT,
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── TRANSACTIONS (Billing / Quotations) ──────────────────────
CREATE TABLE IF NOT EXISTS transactions (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    invoice_number  VARCHAR(50) UNIQUE,
    quotation_number VARCHAR(50) UNIQUE,
    status          VARCHAR(30) NOT NULL DEFAULT 'QUOTATION',
    payment_status  VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    customer_id     BIGINT NOT NULL REFERENCES customers(id),
    gst_enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    tax_all_items   BOOLEAN NOT NULL DEFAULT FALSE,
    gst_type        VARCHAR(20) DEFAULT 'LOCAL',
    subtotal        NUMERIC(14,2) DEFAULT 0,
    total_cgst      NUMERIC(14,2) DEFAULT 0,
    total_sgst      NUMERIC(14,2) DEFAULT 0,
    total_igst      NUMERIC(14,2) DEFAULT 0,
    grand_total     NUMERIC(14,2) DEFAULT 0,
    amount_paid     NUMERIC(14,2) DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    converted_at    TIMESTAMPTZ,
    created_by_id   BIGINT REFERENCES app_users(id)
);

-- ── TRANSACTION ITEMS ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transaction_items (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    transaction_id  BIGINT NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    inventory_item_id BIGINT REFERENCES inventory_items(id),
    description     VARCHAR(300) NOT NULL,
    item_type       VARCHAR(20) NOT NULL,
    hsn_sac_code    VARCHAR(20),
    unit            VARCHAR(20),
    square_feet     NUMERIC(12,3) DEFAULT 0,
    quantity        NUMERIC(12,3) DEFAULT 1,
    rate_per_unit   NUMERIC(12,2) NOT NULL DEFAULT 0,
    gst_percent     NUMERIC(5,2) DEFAULT 0,
    base_amount     NUMERIC(14,2) DEFAULT 0,
    cgst_amount     NUMERIC(14,2) DEFAULT 0,
    sgst_amount     NUMERIC(14,2) DEFAULT 0,
    igst_amount     NUMERIC(14,2) DEFAULT 0,
    total_amount    NUMERIC(14,2) DEFAULT 0
);

-- ── INCOME TRANSACTIONS ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS income_transactions (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    transaction_id  BIGINT REFERENCES transactions(id),
    inventory_number VARCHAR(50) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    amount          NUMERIC(14,2) NOT NULL,
    description     TEXT,
    date            DATE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── PROJECTS ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS projects (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(200) NOT NULL,
    client_name     VARCHAR(200),
    location        VARCHAR(300),
    description     TEXT,
    total_contract_value NUMERIC(14,2) DEFAULT 0,
    start_date      DATE,
    end_date        DATE,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── JOB CARDS ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS job_cards (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            UUID NOT NULL REFERENCES tenants(id),
    project_id           BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    phase                VARCHAR(200) NOT NULL,
    description          TEXT,
    assigned_employee_id BIGINT REFERENCES employees(id),
    status               VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    target_date          DATE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── DAILY LOGS ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS daily_logs (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            UUID NOT NULL REFERENCES tenants(id),
    job_card_id          BIGINT NOT NULL REFERENCES job_cards(id),
    log_date             DATE NOT NULL,
    progress_description TEXT,
    work_value           NUMERIC(14,2) DEFAULT 0,
    number_of_labours    INT NOT NULL DEFAULT 0,
    daily_wage_rate      NUMERIC(10,2) DEFAULT 0,
    total_labour_cost    NUMERIC(14,2) DEFAULT 0,
    logged_by_id         BIGINT REFERENCES app_users(id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── PROJECT EXPENSES ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS project_expenses (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            UUID NOT NULL REFERENCES tenants(id),
    daily_log_id         BIGINT REFERENCES daily_logs(id),
    project_id           BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    job_card_id          BIGINT NOT NULL REFERENCES job_cards(id),
    category             VARCHAR(50) NOT NULL,
    description          VARCHAR(300) NOT NULL,
    amount               NUMERIC(12,2) NOT NULL,
    expense_date         DATE NOT NULL,
    attachment_name      VARCHAR(255),
    attachment_path      VARCHAR(500),
    attachment_mime_type VARCHAR(100),
    status               VARCHAR(30) NOT NULL DEFAULT 'NEW',
    company_expense_id   BIGINT REFERENCES expenses(id),
    submitted_by_id      BIGINT REFERENCES app_users(id),
    approved_by_id       BIGINT REFERENCES app_users(id),
    approved_at          TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── PROJECT LABOUR ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS project_labours (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    project_id      BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    phone           VARCHAR(30),
    default_wage    NUMERIC(10,2) DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── DAILY LABOUR LOG ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS daily_labour_logs (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         UUID NOT NULL REFERENCES tenants(id),
    daily_log_id      BIGINT NOT NULL REFERENCES daily_logs(id) ON DELETE CASCADE,
    project_labour_id BIGINT NOT NULL REFERENCES project_labours(id),
    wage_paid         NUMERIC(10,2) NOT NULL DEFAULT 0,
    status            VARCHAR(30) NOT NULL DEFAULT 'NEW'
);

-- ── ATTENDANCE LEDGER ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS attendance_ledger (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         UUID NOT NULL REFERENCES tenants(id),
    employee_id       BIGINT NOT NULL REFERENCES employees(id),
    date              DATE NOT NULL,
    clock_in_time     TIME,
    clock_out_time    TIME,
    status            VARCHAR(20),
    manual_correction BOOLEAN NOT NULL DEFAULT FALSE,
    admin_notes       VARCHAR(500),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, employee_id, date)
);

-- ── LEAVE BALANCE ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS leave_balances (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    employee_id     BIGINT NOT NULL REFERENCES employees(id),
    year_val        INT NOT NULL,
    allocated_sick_leaves   INT NOT NULL DEFAULT 0,
    used_sick_leaves        INT NOT NULL DEFAULT 0,
    allocated_casual_leaves INT NOT NULL DEFAULT 0,
    used_casual_leaves      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, employee_id, year_val)
);

-- ── LEAVE APPLICATIONS ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS leave_applications (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    employee_id     BIGINT NOT NULL REFERENCES employees(id),
    leave_type      VARCHAR(30) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    reason          TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    applied_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── HOLIDAYS ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS holidays (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(150) NOT NULL,
    date            DATE NOT NULL,
    UNIQUE(tenant_id, date)
);

-- ── PURCHASE ORDERS ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS purchase_orders (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            UUID NOT NULL REFERENCES tenants(id),
    po_number            VARCHAR(50) NOT NULL UNIQUE,
    vendor_id            BIGINT NOT NULL REFERENCES vendors(id),
    status               VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    order_date           DATE,
    expected_delivery_date DATE,
    actual_delivery_date DATE,
    total_amount         NUMERIC(14,2) NOT NULL DEFAULT 0,
    paid                 BOOLEAN NOT NULL DEFAULT FALSE,
    notes                TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── PURCHASE ORDER ITEMS ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS purchase_order_items (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    inventory_item_id BIGINT REFERENCES inventory_items(id),
    description     VARCHAR(500) NOT NULL,
    quantity        NUMERIC(12,3) NOT NULL,
    unit            VARCHAR(20),
    unit_price      NUMERIC(14,2) NOT NULL,
    total_price     NUMERIC(14,2) NOT NULL
);

-- ── COMPANY SETTINGS ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS company_settings (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) UNIQUE,
    company_name    VARCHAR(200),
    address         VARCHAR(500),
    phone           VARCHAR(50),
    email           VARCHAR(100),
    website         VARCHAR(100),
    tax_number      VARCHAR(50),
    default_sick_leaves_per_year INT NOT NULL DEFAULT 10,
    default_casual_leaves_per_year INT NOT NULL DEFAULT 10,
    weekly_off_days VARCHAR(200) DEFAULT 'SUNDAY',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
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
CREATE INDEX IF NOT EXISTS idx_proj_lab_tenant       ON project_labours(tenant_id);
CREATE INDEX IF NOT EXISTS idx_daily_log_tenant      ON daily_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_daily_lab_tenant      ON daily_labour_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_attendance_tenant     ON attendance_ledger(tenant_id);
CREATE INDEX IF NOT EXISTS idx_leave_bal_tenant      ON leave_balances(tenant_id);
CREATE INDEX IF NOT EXISTS idx_leave_app_tenant      ON leave_applications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_holidays_tenant       ON holidays(tenant_id);
CREATE INDEX IF NOT EXISTS idx_po_tenant             ON purchase_orders(tenant_id);
CREATE INDEX IF NOT EXISTS idx_po_items_tenant       ON purchase_order_items(tenant_id);
