--liquibase formatted sql
-- changeset system:V12-add-vendor-fields
ALTER TABLE vendors ADD COLUMN IF NOT EXISTS bank_account_name VARCHAR(200);
ALTER TABLE vendors ADD COLUMN IF NOT EXISTS notes TEXT;
