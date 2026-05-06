--liquibase formatted sql
-- changeset system:V11-fix-unique-constraints

-- 1. Fix transactions table
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_invoice_number_key;
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_quotation_number_key;
ALTER TABLE transactions ADD CONSTRAINT uk_tenant_invoice_number UNIQUE (tenant_id, invoice_number);
ALTER TABLE transactions ADD CONSTRAINT uk_tenant_quotation_number UNIQUE (tenant_id, quotation_number);

-- 2. Fix employees table
ALTER TABLE employees DROP CONSTRAINT IF EXISTS employees_employee_code_key;
ALTER TABLE employees ADD CONSTRAINT uk_tenant_employee_code UNIQUE (tenant_id, employee_code);

-- 3. Fix purchase_orders table
ALTER TABLE purchase_orders DROP CONSTRAINT IF EXISTS purchase_orders_po_number_key;
ALTER TABLE purchase_orders ADD CONSTRAINT uk_tenant_po_number UNIQUE (tenant_id, po_number);
