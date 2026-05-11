# Super ERP

Super ERP is a Spring Boot 3.x multi-tenant ERP application with modules for billing, inventory, CRM, HR, expenses, projects, SCM, approvals, finance, RBAC, tenant administration, and file uploads.

Current status: **not production ready yet**. See [production-readiness-implementation-plan.md](production-readiness-implementation-plan.md) before deploying publicly.

---

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+ recommended for local development and production-like testing

### Run Locally

```bash
mvn spring-boot:run
```

Open:

- App: http://localhost:8085
- Login: http://localhost:8085/login
- System login: http://localhost:8085/system/login

The default port is configured in `src/main/resources/application.properties`.

---

## Configuration

The current development configuration lives in:

```text
src/main/resources/application.properties
```

Important settings:

```properties
server.port=8085
spring.datasource.url=jdbc:postgresql://localhost:5432/super_erp
spring.datasource.username=erp_app
spring.datasource.password=admin123
app.jwt.secret=super-erp-jwt-secret-key-must-be-256-bits-long-for-hs256-algorithm
app.system.admin.username=superadmin
app.system.admin.password=SuperAdmin@2026!
```

Before production, move all secrets to environment variables and rotate any values committed to the repository.

Recommended production pattern:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
app.jwt.secret=${APP_JWT_SECRET}
app.system.admin.username=${SYSTEM_ADMIN_USERNAME}
app.system.admin.password=${SYSTEM_ADMIN_PASSWORD}
```

---

## Database

The application is configured for PostgreSQL.

Default local database:

```text
Database: super_erp
User: erp_app
```

Liquibase migrations are loaded from:

```text
src/main/resources/db/changelog/master.xml
```

### Required PostgreSQL Extension: `pg_trgm`

`V17__search_indexes.sql` can create optional trigram indexes for faster `LIKE '%query%'` searches. These indexes require the PostgreSQL extension `pg_trgm`.

Normal application users often do not have permission to create extensions. If you see this error:

```text
ERROR: permission denied to create extension "pg_trgm"
```

run this once as a privileged PostgreSQL user, usually `postgres`:

```bash
psql -U postgres -d super_erp -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"
```

If PostgreSQL is running in Docker:

```bash
docker exec -it <postgres_container_name> psql -U postgres -d super_erp -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"
```

The migration is written so the app can still start without `pg_trgm`; normal B-tree indexes are created, and trigram indexes are skipped unless the extension already exists.

---

## Docker

```bash
docker-compose up --build -d
docker-compose logs -f
docker-compose down
```

Note: the current Docker Compose file should be reviewed before production use. The production readiness plan recommends switching Compose to a PostgreSQL service and using environment variables or a secret manager for credentials.

---

## Build and Test

Run tests:

```bash
mvn test
```

Build the JAR:

```bash
mvn clean package -DskipTests
```

Run the JAR:

```bash
java -jar target/erp-1.0.0.jar
```

The current test suite is mostly a Spring context-load check. Add integration tests for authorization, tenant isolation, CSRF, JWT logout, file upload/download, and PostgreSQL migrations before production.

---

## Key Modules

- Multi-tenant foundation
- System tenant administration
- RBAC roles and permissions
- Billing and quotations
- Customers and vendors
- Inventory
- Employees, salary, HR attendance, leave management
- Expenses and file attachments
- Projects and job cards
- Finance dashboard and ledger
- SCM purchase orders
- Approval queue and analytics
- Public enquiry submission

---

## Security Notes

Before production:

- Remove hardcoded secrets.
- Disable tenant header fallback for public deployments.
- Re-enable CSRF for browser-based tenant administration routes.
- Add explicit permissions to all sensitive business controllers.
- Fix JWT logout token blacklisting.
- Harden file upload/download path handling.
- Use PostgreSQL-backed integration tests.

See [production-readiness-implementation-plan.md](production-readiness-implementation-plan.md) for the full checklist.

---

## Tech Stack

| Component | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2 |
| Security | Spring Security, BCrypt, JWT, RBAC |
| Database | PostgreSQL |
| Migrations | Liquibase |
| ORM | Spring Data JPA / Hibernate |
| UI | Thymeleaf + Bootstrap |
| Cache | Caffeine |
| PDF | iText 5 |
| Build | Maven |
| Container | Docker |

