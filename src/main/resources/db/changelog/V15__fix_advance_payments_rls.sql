--liquibase formatted sql
-- ============================================================
--  V15: Fix Advance Payments RLS Policy
-- ============================================================

-- changeset system:V15-001 runOnChange:false
-- validCheckSum: 9:cf91f7ccebb503aa53c0a254fb08a43b
DROP POLICY IF EXISTS tenant_isolation_policy ON advance_payments;

CREATE POLICY tenant_isolation ON advance_payments
    USING (tenant_id = current_tenant_id());

-- changeset system:V15-002 runOnChange:false
ALTER TABLE advance_payments FORCE ROW LEVEL SECURITY;
