# Goal
Finalize the production deployment configuration by moving to PostgreSQL in Docker, adding health checks via Actuator, and implementing startup validation for critical secrets.

## User Review Required

> [!IMPORTANT]
> - **PostgreSQL in Docker**: The `docker-compose.yml` will be updated to include a PostgreSQL container. This will replace the H2 file-based storage currently in the Compose file.
> - **Actuator Endpoints**: We will expose `/health` and `/info` endpoints. By default, these are restricted, but we will ensure they are accessible for container health checks.

## Proposed Changes

### 1. Observability and Health Checks
#### [MODIFY] `pom.xml`
- Add `spring-boot-starter-actuator` dependency.
#### [MODIFY] `src/main/resources/application.properties` (and `-prod.properties`)
- Configure Actuator to expose health and info.
- Enable detailed health information for the database.

### 2. Docker/PostgreSQL Deployment
#### [MODIFY] `docker-compose.yml`
- Add `db` service using `postgres:15-alpine`.
- Link `super-erp-app` to the `db` service.
- Update environment variables to point to the PostgreSQL container.
- Add health check dependencies (app waits for DB).
#### [MODIFY] `Dockerfile`
- Ensure the app starts with the `prod` profile by default or via environment variable.

### 3. Startup Validation
#### [NEW] `src/main/java/com/supererp/erp/config/StartupValidator.java`
- Implement `CommandLineRunner` or `ApplicationListener`.
- Check if critical environment variables are set when the `prod` profile is active:
    - `JWT_SECRET`
    - `SPRING_DATASOURCE_PASSWORD`
    - `APP_SYSTEM_ADMIN_PASSWORD`
- Throw a meaningful exception to "fail fast" if secrets are missing in production.

## Verification Plan

### Automated/Manual Verification
1. **Docker Build**: Run `docker compose build` and `docker compose up -d`.
2. **Connectivity**: Verify the app successfully connects to the PostgreSQL container and runs Liquibase migrations.
3. **Health Checks**: Access `http://localhost:8085/actuator/health` and verify the status is `UP`.
4. **Startup Failure**: Try starting the app in `prod` mode without a `JWT_SECRET` and verify it exits with an error message.
