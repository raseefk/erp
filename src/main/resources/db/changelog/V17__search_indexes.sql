--liquibase formatted sql

--changeset supererp:V17-1-search-btree-indexes dbms:postgresql
--comment: B-tree indexes for common tenant-scoped filters and foreign keys.

CREATE INDEX IF NOT EXISTS idx_transactions_tenant_created ON transactions (tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_income_trans_tenant_date ON income_transactions (tenant_id, date);
CREATE INDEX IF NOT EXISTS idx_expenses_tenant_date ON expenses (tenant_id, expense_date);
CREATE INDEX IF NOT EXISTS idx_attendance_tenant_date ON attendance_ledger (tenant_id, date);
CREATE INDEX IF NOT EXISTS idx_leave_tenant_start ON leave_applications (tenant_id, start_date);
CREATE INDEX IF NOT EXISTS idx_app_users_tenant ON app_users (tenant_id);

--changeset supererp:V17-2-search-trigram-indexes dbms:postgresql
--comment: Optional trigram indexes. Requires pg_trgm to be installed by a privileged DBA/user before this migration runs.
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM pg_extension WHERE extname = 'pg_trgm'

CREATE INDEX IF NOT EXISTS trgm_idx_vendors_name ON vendors USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS trgm_idx_projects_name ON projects USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS trgm_idx_items_name ON inventory_items USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS trgm_idx_users_name ON app_users USING gin (lower(full_name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS trgm_idx_customers_name ON customers USING gin (lower(name) gin_trgm_ops);
