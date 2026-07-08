# Oracle Database Setup Guide for Super ERP

## Overview
The application is configured for **single-tenant mode** with Oracle Autonomous Database. Liquibase is disabled due to OracleADB parallel transaction limitations (ORA-12838). Instead, the schema must be created manually.

## Prerequisites
- Oracle Autonomous Database (ADB) instance with TCPS connection
- SQL*Plus, Oracle SQL Developer, or DBeaver with Oracle driver
- Connection credentials (username: `super_erp_db_admin_user`, password in `application.properties`)

## Setup Steps

### Step 1: Connect to Oracle Database
Using SQL Developer or SQL*Plus:
```bash
# If using SQL*Plus
sqlplus super_erp_db_admin_user@(description= (retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1522)(host=adb.ap-hyderabad-1.oraclecloud.com))(connect_data=(service_name=g0640c7a3df714d_mthjttv6ulke3bjy_high.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))
```

Or in SQL Developer:
- **Connection Name**: Super ERP
- **Username**: `super_erp_db_admin_user`
- **Password**: (from `application.properties`)
- **Connection Type**: Cloud Wallet
- **Configuration File**: Your ADB wallet zip file
- **Keystore Password**: Your wallet password

### Step 2: Run Schema Creation Script
Execute the manual schema setup script:
```sql
@setup_schema_manual.sql
```

Or run it directly in SQL Developer/DBeaver:
1. Open `setup_schema_manual.sql`
2. Execute it (Ctrl+Enter or Run button)
3. Wait for completion confirmation

### Step 3: Seed Initial Data (Optional)
Once schema is created, the application's `DataInitializer` will automatically:
- Create the single application tenant record (if not exists)
- Create ADMIN and EMPLOYEE system roles
- Create the admin user (`admin` / `Admin@1234`)
- Enable all features at application level

No manual data insertion is needed—just start the application!

### Step 4: Start the Application
```bash
java -jar target/erp-1.0.0.jar
```

Or using Maven:
```bash
mvn spring-boot:run
```

### Step 5: Verify Startup
Check logs for:
```
✅ Application tenant seeded
✅ ADMIN role created
✅ EMPLOYEE role created
✅ Admin user 'admin' created
✅ All features enabled at application level
✅ DataInitializer complete
2026-07-08T23:42:32.094+05:30  INFO 25524 --- Tomcat initialized on port 8085
```

## Access the Application

### Login
- **URL**: http://localhost:8085/login
- **Username**: `admin`
- **Password**: `Admin@1234`

### Admin Dashboard
- **URL**: http://localhost:8085/admin/dashboard

### System Health & Metrics
- **URL**: http://localhost:8085/app-admin
- **Real-time Stats**: http://localhost:8085/app-admin/api/stats (SSE stream)

## Configuration

### Database Connection
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:oracle:thin:@(description=...)
spring.datasource.username=super_erp_db_admin_user
spring.datasource.password=Str0ngP@s$worD123!
```

### DDL Settings
Currently configured for:
```properties
spring.jpa.hibernate.ddl-auto=validate
spring.liquibase.enabled=false
```

**Do NOT change these** unless you're troubleshooting schema creation.

## Troubleshooting

### Error: ORA-00942: table or view does not exist
**Cause**: Schema not created before starting application
**Solution**: Run `setup_schema_manual.sql` before starting

### Error: ORA-00001: unique constraint violated
**Cause**: Tenant record already exists in database
**Solution**: This is normal on restart. DataInitializer checks and skips if exists.

### Error: ORA-12838: cannot read/modify an object after modifying it in parallel
**Cause**: Liquibase tried to create schema (disabled now)
**Solution**: Already fixed. Liquibase is disabled (`spring.liquibase.enabled=false`)

### Error: Cannot connect to Oracle database
**Cause**: Wrong credentials or connection string
**Solution**: Verify in `application.properties` and test connection in SQL Developer first

## File Locations

- **Main JAR**: `target/erp-1.0.0.jar`
- **Schema Script**: `setup_schema_manual.sql`
- **Config**: `src/main/resources/application.properties`
- **Liquibase Migrations**: `src/main/resources/db/changelog/` (disabled, for reference only)

## Tenant Configuration

This is a **single-tenant application** with a fixed application tenant:
- **Tenant ID**: Defined in `src/main/java/com/supererp/erp/config/AppTenantConfig.java`
- **All data belongs to**: The single "Super ERP" tenant
- **Multi-tenancy features**: Removed entirely
- **RLS (Row-Level Security)**: Disabled (app-level filtering only)

## Next Steps

1. ✅ Run `setup_schema_manual.sql` to create schema
2. ✅ Start application: `java -jar target/erp-1.0.0.jar`
3. ✅ Login with `admin` / `Admin@1234`
4. ✅ Configure company settings in `/app-admin`
5. ✅ Add users and assign roles as needed

## Support

For detailed logs, set `logging.level.com.supererp=DEBUG` in `application.properties`

---
**Last Updated**: July 8, 2026  
**Version**: Super ERP 1.0.0 (Single-Tenant Oracle)
