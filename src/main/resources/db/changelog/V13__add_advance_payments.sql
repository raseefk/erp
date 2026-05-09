--liquibase formatted sql
-- ============================================================
--  V13: Add Advance Payments
-- ============================================================

-- changeset system:V13-001-v1 runOnChange:false

-- ── ADVANCE PAYMENTS ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS advance_payments (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    advance_number  VARCHAR(50) NOT NULL UNIQUE,
    payment_from    VARCHAR(200) NOT NULL,
    project_id      BIGINT REFERENCES projects(id),
    amount          NUMERIC(14,2) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    description     TEXT,
    date            DATE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_advance_payments_tenant ON advance_payments(tenant_id);

ALTER TABLE advance_payments ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON advance_payments
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

CREATE POLICY system_admin_bypass ON advance_payments
    FOR ALL
    USING (current_setting('app.is_system_admin', true) = 'true');

-- ── ALTER TRANSACTIONS (BILLS) ────────────────────────────────
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS project_id BIGINT REFERENCES projects(id);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS advance_payment_id BIGINT REFERENCES advance_payments(id);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS advance_settled_amount NUMERIC(14,2) DEFAULT 0;

-- ── ALTER INCOME TRANSACTIONS ─────────────────────────────────
ALTER TABLE income_transactions ALTER COLUMN transaction_id DROP NOT NULL;
ALTER TABLE income_transactions ADD COLUMN IF NOT EXISTS advance_payment_id BIGINT REFERENCES advance_payments(id);

