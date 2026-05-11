# Goal
Implement the Performance Improvement Plan to optimize the application's reporting, dashboards, file storage tracking, and search performance.

## User Review Required

> [!IMPORTANT]
> - **Liquibase Migrations**: The changes for tracking tenant upload size and adding `pg_trgm` indexes will introduce two new Liquibase migrations (`V16` and `V17`).
> - **Database Privileges**: Enabling the `pg_trgm` extension in PostgreSQL usually requires database superuser privileges. If your `erp_app` database user does not have these privileges, the `V17` migration may fail. Please ensure the user has the right privileges or let me know if we should skip the `pg_trgm` extension creation.

## Proposed Changes

### 1. Optimize HR Attendance Reports
#### [MODIFY] `src/main/java/com/supererp/erp/service/HrService.java`
- Refactor `getAttendanceReport()` to fetch all attendances, leaves, and holidays for the requested date range in three bulk queries (one for each entity type).
- Group the fetched data in-memory by `employeeId` and `date` using Maps.
- Assemble the `AttendanceReportDto` list by iterating through the dates and fetching from the Maps, eliminating the N+1 query loop.

### 2. Optimize Finance Dashboard Aggregations
#### [MODIFY] `src/main/java/com/supererp/erp/repository/IncomeTransactionRepository.java` (and Expense/Salary Repos)
- Add `@Query` methods to fetch aggregated sums grouped by month/year.
#### [MODIFY] `src/main/java/com/supererp/erp/service/ProfitLossService.java`
- Update `last12Months()` to execute the bulk group-by queries and merge the results into the 12-month summary list, eliminating the 36 separate queries.

### 3. Avoid Repeated Filesystem Walks for Tenant Upload Usage
#### [NEW] `src/main/resources/db/changelog/V16__add_upload_size_bytes.sql`
- Add `upload_size_bytes` column to the `tenants` table.
#### [MODIFY] `src/main/java/com/supererp/erp/tenant/Tenant.java` & `TenantService.java`
- Add the `uploadSizeBytes` field to the entity and a method to update it.
#### [MODIFY] `src/main/java/com/supererp/erp/service/FileStorageService.java`
- Update `store()` and `delete()` methods to dynamically update the tenant's `uploadSizeBytes`. Remove the recursive filesystem walk logic from `getTenantUploadSizeInGB()`.

### 4. Remove System Dashboard N+1 Queries
#### [MODIFY] `src/main/java/com/supererp/erp/repository/AppUserRepository.java`
- Add a query to fetch user counts grouped by `tenant_id`.
#### [MODIFY] `src/main/java/com/supererp/erp/controller/system/SystemAdminController.java`
- Update `systemDashboard()` to use the bulk user count query and the `uploadSizeBytes` field directly from the tenant entity, removing the N+1 queries.

### 5. Improve Search Performance
#### [NEW] `src/main/resources/db/changelog/V17__search_indexes.sql`
- Add `CREATE EXTENSION IF NOT EXISTS pg_trgm;`.
- Add GIN trigram indexes on heavily searched text fields (e.g., `vendors.name`, `projects.name`, `inventory_items.name`, `app_users.name`, `customers.name`).
- Add standard B-tree indexes for `tenant_id` and `date` columns to speed up range queries.

### 6. Reduce Test Log Noise
#### [NEW] `src/test/resources/application-test.properties`
- Create a test profile configuration that disables Hibernate SQL logging and reduces log levels for Hibernate binders to suppress the repeated PostgreSQL RLS `set_config` warnings during `mvn test`.

## Verification Plan

### Automated/Manual Verification
1. **Build and Test**: Run `mvn clean test` to ensure tests pass and log noise is reduced.
2. **Dashboard Performance**: Load the System Admin dashboard and the Finance dashboard to verify they load instantly without generating N+1 queries in the logs.
3. **HR Report Accuracy**: Generate the HR attendance report and verify the data matches expected outputs and loads significantly faster.
4. **File Storage Quotas**: Upload and delete files as a tenant to ensure the `uploadSizeBytes` correctly tracks usage without walking the filesystem.
