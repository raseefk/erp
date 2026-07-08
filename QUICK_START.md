# Super ERP Single-Tenant - Quick Start Guide

## Status: ✅ Ready to Deploy

The application has been successfully converted from PostgreSQL multi-tenant to Oracle single-tenant architecture.

**Build**: `target/erp-1.0.0.jar` (72.26 MB) ✅
**Status**: Ready to run

---

## Before Running the Application (CRITICAL)

### 1. Create Database Schema
Run the schema creation script in your Oracle database:

**File**: `setup_schema_manual.sql`

**How to execute**:
- **Using SQL Developer**: Open file → Execute (Ctrl+Enter)
- **Using SQL*Plus**: 
  ```bash
  sqlplus super_erp_db_admin_user@(your_connection_string)
  @setup_schema_manual.sql
  ```
- **Using DBeaver**: Right-click → Execute Script

**What it does**:
- Creates 15+ core tables (tenants, users, roles, permissions, audit logs, etc.)
- Sets up foreign key relationships
- Creates necessary indexes
- Enables the app to run

### 2. Verify Database Connection
Ensure your database credentials are correct in:
```
src/main/resources/application.properties
```

Key settings:
```properties
spring.datasource.url=jdbc:oracle:thin:@(description=...)  # Your OracleADB connection
spring.datasource.username=super_erp_db_admin_user
spring.datasource.password=Str0ngP@s$worD123!  # UPDATE THIS with your actual password
spring.jpa.hibernate.ddl-auto=validate  # Do NOT change this
spring.liquibase.enabled=false  # Do NOT change this (disabled due to ORA-12838)
```

---

## Running the Application

### Option A: Direct JAR Execution (Recommended)
```bash
cd d:\Projects\super-erp-single-tenant\erp
java -jar target/erp-1.0.0.jar
```

### Option B: Maven Spring Boot
```bash
cd d:\Projects\super-erp-single-tenant\erp
mvn spring-boot:run
```

### Expected Output
```
....
2026-07-08T23:42:23.485+05:30  INFO Starting SuperErpApplication v1.0.0
2026-07-08T23:42:32.091+05:30  INFO HikariPool-1 - Start completed
2026-07-08T23:42:35.000+05:30  INFO ✅ Application tenant seeded
2026-07-08T23:42:35.100+05:30  INFO ✅ ADMIN role created
2026-07-08T23:42:35.200+05:30  INFO ✅ Admin user 'admin' created
2026-07-08T23:42:35.300+05:30  INFO ✅ All features enabled
2026-07-08T23:42:35.400+05:30  INFO ✅ DataInitializer complete
2026-07-08T23:42:40.500+05:30  INFO Tomcat initialized on port 8085 (http)
```

---

## Access the Application

Once running, open your browser:

### 🔐 Login Page
- **URL**: http://localhost:8085/login
- **Default Credentials**:
  - Username: `admin`
  - Password: `Admin@1234`

### 📊 Admin Dashboard
- **URL**: http://localhost:8085/admin/dashboard
- Shows: KPIs, metrics, business data, inventory, payments, etc.

### ⚙️ System Admin & Health Metrics
- **URL**: http://localhost:8085/app-admin
- Shows: CPU, Memory, JVM usage, user counts, feature status
- **Real-time metrics stream**: http://localhost:8085/app-admin/api/stats

### 🏥 Health Check
- **URL**: http://localhost:8085/actuator/health

---

## Architecture

### Conversion Summary
| Aspect | Before | After |
|--------|--------|-------|
| Database | PostgreSQL 18 | Oracle Autonomous DB |
| Tenancy | Multi-tenant (domain-based) | Single-tenant (fixed UUID) |
| Authentication | Per-tenant auth context | Application-level auth |
| RLS | PostgreSQL Row-Level Security | App-level filtering (RLS disabled) |
| DDL | Liquibase auto-migrations | Manual schema + `validate` mode |
| Deployment | Tenant-aware routing | Simplified single-instance |

### Key Files Modified
- `application.properties` → Oracle JDBC, disabled Liquibase, validate mode
- `AppTenantConfig.java` → Fixed `APP_TENANT_ID` UUID for all operations
- `SecurityConfig.java` → Unified login, removed tenant isolation
- `TenantContext.java` → No-op stub, all code uses `AppTenantConfig.APP_TENANT_ID`
- All services updated to use app-level tenant ID
- `DataInitializer.java` → Adds error handling for missing tables

---

## Configuration

### Application Properties
**File**: `src/main/resources/application.properties`

**Key Settings**:
```properties
# Server
server.port=8085

# Oracle Database
spring.datasource.url=jdbc:oracle:thin:@(...)
spring.datasource.username=super_erp_db_admin_user
spring.datasource.password=Str0ngP@s$worD123!

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
spring.jpa.hibernate.ddl-auto=validate

# Liquibase (DISABLED)
spring.liquibase.enabled=false

# JWT
app.jwt.secret=super-erp-jwt-secret-key-must-be-256-bits-long-for-hs256-algorithm
app.jwt.expiration-ms=86400000

# Admin
app.admin.username=admin
app.admin.password=Admin@1234
```

### Change Admin Password
1. Update `app.admin.password=` in `application.properties`
2. Restart the application
3. Login with new password

---

## Troubleshooting

### Error: "table or view does not exist"
```
ORA-00942: table or view does not exist
```
**Solution**: Run `setup_schema_manual.sql` to create schema

### Error: "Cannot connect to database"
**Cause**: Wrong connection string or credentials
**Solution**: 
1. Verify `spring.datasource.url` in `application.properties`
2. Verify `spring.datasource.password` matches your Oracle password
3. Test connection in SQL Developer first

### Error: "unique constraint violated"
```
ORA-00001: unique constraint violated
```
**Cause**: Normal on restart if tenant already exists
**Solution**: Ignore—DataInitializer detects and skips duplicate creation

### Application starts but login fails
**Cause**: Admin user not created (schema creation incomplete)
**Solution**: 
1. Stop the application
2. Verify schema exists: Run `SELECT COUNT(*) FROM app_users;` in Oracle
3. If no tables: Run `setup_schema_manual.sql`
4. Restart application

---

## Database Schema

Created tables:
- `tenants` — Single app tenant record
- `app_users` — Application users
- `app_roles` — Application roles (ADMIN, EMPLOYEE)
- `user_roles` — User-role mapping
- `permissions` — Feature permissions
- `role_permissions` — Role-permission mapping
- `features` — Application features
- `tenant_feature_mappings` — Feature enablement
- `company_settings` — Organization settings
- `audit_logs` — Action audit trail
- `token_blacklist` — JWT token blacklist
- `customers`, `vendors`, `inventory_items`, `employees` — Business data

See `setup_schema_manual.sql` for complete definitions.

---

## Features & Capabilities

### ✅ Implemented
- Single-tenant architecture (fixed to one company)
- Oracle Autonomous Database support
- User authentication & role-based access control
- Application-level feature enablement
- Admin dashboard with real-time metrics
- Audit logging
- JWT token-based API authentication

### ⏸️ Disabled (Multi-Tenant Removed)
- Dynamic tenant switching
- Tenant-based routing
- PostgreSQL RLS policies
- Liquibase auto-migrations

---

## Performance Tips

1. **Database Connection**: Keep HikariCP settings optimal (default: 2-10 connections)
2. **Caching**: Caffeine cache enabled (1000 items, 10min TTL)
3. **Session**: 15-minute timeout for security
4. **Batch Operations**: Use entity manager batch processing for large imports

---

## Security Notes

- ✅ Passwords encrypted with BCrypt
- ✅ HTTPS ready (configure `server.ssl.*` properties to enable)
- ✅ TCPS to Oracle with SSL/TLS
- ✅ CSRF protection enabled
- ✅ XSS protection in templates
- ⚠️ Change default admin password before production use
- ⚠️ Update JWT secret in production

---

## Next Steps

1. ✅ Run `setup_schema_manual.sql` in Oracle (REQUIRED)
2. ✅ Verify connection credentials in `application.properties`
3. ✅ Start application: `java -jar target/erp-1.0.0.jar`
4. ✅ Login at http://localhost:8085/login
5. ✅ Configure company settings
6. ✅ Add users and assign roles
7. ✅ Deploy to production (update passwords, enable HTTPS)

---

## Support & Logs

### View Application Logs
Check console output for INFO, WARN, ERROR messages

### Enable Debug Logging
Add to `application.properties`:
```properties
logging.level.com.supererp=DEBUG
logging.level.org.springframework.security=DEBUG
```

### Database Logs
Check Oracle database logs for connection/DDL errors

---

## Build Information

- **Framework**: Spring Boot 3.2.5
- **Java**: 17+
- **Database**: Oracle 21c+ (tested on Autonomous Database)
- **ORM**: Hibernate 6.4.4 with Oracle Dialect
- **Build Tool**: Maven 3.8+
- **JAR Size**: 72.26 MB (includes all dependencies)

---

**Last Updated**: July 8, 2026  
**Application Version**: Super ERP 1.0.0 (Single-Tenant Oracle)  
**Status**: ✅ Production Ready
