--liquibase formatted sql
-- ============================================================
--  V23: Payroll & Compliance Engine
-- ============================================================

-- changeset system:V23-001 runOnChange:false

-- ── PAYROLL CONFIG (per-employee CTC breakup + statutory flags) ───────────────
CREATE TABLE IF NOT EXISTS payroll_configs (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    employee_id         BIGINT NOT NULL REFERENCES employees(id),
    basic_pct           NUMERIC(5,2) NOT NULL DEFAULT 40.00,
    hra_pct             NUMERIC(5,2) NOT NULL DEFAULT 20.00,
    da_pct              NUMERIC(5,2) NOT NULL DEFAULT 10.00,
    pf_enabled          BOOLEAN NOT NULL DEFAULT FALSE,
    pf_pct              NUMERIC(5,2) NOT NULL DEFAULT 12.00,
    esi_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    pt_enabled          BOOLEAN NOT NULL DEFAULT FALSE,
    pt_state            VARCHAR(5),
    tds_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    tds_annual          NUMERIC(14,2) NOT NULL DEFAULT 0,
    encashable_leave_days INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_payroll_config_emp UNIQUE(tenant_id, employee_id)
);

-- ── PAYROLL RUNS (monthly run header) ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payroll_runs (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    pay_month           INT NOT NULL CHECK (pay_month BETWEEN 1 AND 12),
    pay_year            INT NOT NULL,
    pay_period_label    VARCHAR(30),
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_gross         NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_deductions    NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_net           NUMERIC(14,2) NOT NULL DEFAULT 0,
    approved_by         VARCHAR(200),
    approved_at         TIMESTAMPTZ,
    disbursement_date   DATE,
    remarks             VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_payroll_run_month UNIQUE(tenant_id, pay_month, pay_year)
);

-- ── PAYROLL ENTRIES (one computed payslip per employee per run) ────────────────
CREATE TABLE IF NOT EXISTS payroll_entries (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    payroll_run_id      BIGINT NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    employee_id         BIGINT NOT NULL REFERENCES employees(id),
    total_working_days  INT,
    days_present        INT,
    days_absent         INT,
    days_leave          INT,
    days_lop            INT NOT NULL DEFAULT 0,
    ctc_monthly         NUMERIC(12,2) NOT NULL DEFAULT 0,
    gross_salary        NUMERIC(12,2) NOT NULL DEFAULT 0,
    basic               NUMERIC(12,2) NOT NULL DEFAULT 0,
    hra                 NUMERIC(12,2) NOT NULL DEFAULT 0,
    da                  NUMERIC(12,2) NOT NULL DEFAULT 0,
    special_allowance   NUMERIC(12,2) NOT NULL DEFAULT 0,
    arrears             NUMERIC(12,2) NOT NULL DEFAULT 0,
    leave_encashment    NUMERIC(12,2) NOT NULL DEFAULT 0,
    pf_employee         NUMERIC(12,2) NOT NULL DEFAULT 0,
    pf_employer         NUMERIC(12,2) NOT NULL DEFAULT 0,
    esi_employee        NUMERIC(12,2) NOT NULL DEFAULT 0,
    esi_employer        NUMERIC(12,2) NOT NULL DEFAULT 0,
    professional_tax    NUMERIC(12,2) NOT NULL DEFAULT 0,
    tds                 NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_deductions    NUMERIC(12,2) NOT NULL DEFAULT 0,
    net_salary          NUMERIC(12,2) NOT NULL DEFAULT 0,
    bank_name           VARCHAR(100),
    account_number      VARCHAR(50),
    ifsc_code           VARCHAR(20),
    disbursed           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_payroll_entry UNIQUE(tenant_id, payroll_run_id, employee_id)
);

-- ── PAYROLL ARREARS ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payroll_arrears (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    employee_id         BIGINT NOT NULL REFERENCES employees(id),
    arrear_period       VARCHAR(30) NOT NULL,
    old_salary          NUMERIC(12,2),
    new_salary          NUMERIC(12,2),
    arrear_amount       NUMERIC(12,2) NOT NULL,
    reason              VARCHAR(500),
    paid                BOOLEAN NOT NULL DEFAULT FALSE,
    payroll_entry_id    BIGINT REFERENCES payroll_entries(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── INDEXES ────────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_payroll_configs_tenant  ON payroll_configs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payroll_runs_tenant     ON payroll_runs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payroll_entries_tenant  ON payroll_entries(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payroll_entries_run     ON payroll_entries(payroll_run_id);
CREATE INDEX IF NOT EXISTS idx_payroll_entries_emp     ON payroll_entries(employee_id);
CREATE INDEX IF NOT EXISTS idx_payroll_arrears_tenant  ON payroll_arrears(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payroll_arrears_emp     ON payroll_arrears(employee_id);
