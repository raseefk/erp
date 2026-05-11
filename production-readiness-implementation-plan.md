# Production Readiness Implementation Plan

Project: Super ERP
Created: 2026-05-11
Goal: Move the application from development/demo readiness to a secure, deployable, supportable production release.

## Production Readiness Verdict

Current status: Not production ready.

The application builds and the current test suite passes, but production blockers remain in secret management, authorization coverage, tenant isolation, CSRF protection, JWT logout behavior, file handling, deployment configuration, and test depth.

## Release Gate Criteria

The application should not be deployed to production until all critical items below are complete:

- No real or reusable secrets are committed.
- Production starts only from environment variables or a secret manager.
- All sensitive business routes enforce explicit permissions.
- Tenant resolution cannot be controlled by untrusted request headers.
- Browser-based admin actions are protected by CSRF.
- JWT logout reliably revokes active tokens.
- File upload/download paths are bounded to the upload root.
- Docker deployment uses the intended production database.
- A production profile exists with safe defaults.
- Integration tests cover auth, authorization, tenant isolation, and critical workflows.

## Phase 1: Configuration and Secret Management

Priority: Critical

### Tasks

1. Replace hardcoded secrets in `src/main/resources/application.properties`.
2. Move database username, database password, JWT secret, and system admin credentials to environment variables.
3. Add `application-prod.properties` with production-safe defaults.
4. Add `.env.example` with placeholder values only.
5. Rotate all currently committed credentials:
   - PostgreSQL password
   - JWT signing secret
   - system admin password
   - any Docker Compose admin passwords
6. Remove credential values from logs.
7. Update `README.md` with production environment variable setup.

### Acceptance Criteria

- `application.properties` contains no real passwords or JWT secrets.
- Application fails fast if required production secrets are missing.
- No log message prints generated or configured passwords.
- `.env.example` contains placeholders, not usable credentials.

## Phase 2: Production Profile

Priority: Critical

### Tasks

1. Create a `prod` profile.
2. Enable Thymeleaf template caching in production.
3. Disable SQL/debug logging in production.
4. Configure secure cookie behavior:
   - `Secure`
   - `HttpOnly`
   - `SameSite`
5. Configure trusted proxy headers only if the deployment uses a reverse proxy.
6. Set production upload directory explicitly through environment variables.
7. Add startup validation for required properties.

### Acceptance Criteria

- Production can start with `SPRING_PROFILES_ACTIVE=prod`.
- Template caching is enabled in prod.
- Debug SQL logs are disabled in prod.
- Session cookies use production-safe settings.

## Phase 3: Tenant Isolation Hardening

Priority: Critical

### Tasks

1. Disable `X-Tenant-ID` header fallback in production.
2. Allow tenant resolution in production only from:
   - trusted subdomain/host, or
   - authenticated JWT tenant claim.
3. If a reverse proxy injects tenant headers, strip incoming client tenant headers at the proxy.
4. Reduce tenant resolution logging from `info` to `debug`.
5. Add tests for:
   - valid tenant subdomain
   - missing tenant
   - invalid tenant
   - forged tenant header
   - JWT tenant mismatch

### Acceptance Criteria

- External clients cannot select tenant context using `X-Tenant-ID` in prod.
- JWT tenant mismatch returns forbidden and revokes the token.
- Tenant logs do not leak excessive request details at info level.

## Phase 4: Authorization Coverage

Priority: Critical

### Tasks

1. Inventory all controllers and routes.
2. Map each route to one explicit permission.
3. Add `@RequiresPermission` or `@PreAuthorize` to all sensitive routes:
   - finance
   - HR
   - salary
   - customers
   - vendors
   - inventory
   - billing
   - projects
   - payments
   - approvals
   - users
   - settings
   - purchase orders
4. Fix system admin role check if needed:
   - prefer `hasRole('SYSTEM_ADMIN')`
   - or use `hasAuthority('ROLE_SYSTEM_ADMIN')`
5. Add authorization integration tests for low-privilege users.
6. Add tests confirming allowed users can still access their permitted workflows.

### Acceptance Criteria

- No business controller relies only on `.authenticated()`.
- Low-privilege users receive `403` for unauthorized CRUD actions.
- Permission changes are reflected in menu visibility and backend enforcement.

## Phase 5: CSRF and Browser Security

Priority: Critical

### Tasks

1. Re-enable CSRF for `/system/tenants/**`.
2. Keep CSRF disabled only for stateless token APIs.
3. Verify all Thymeleaf forms include CSRF tokens.
4. Move inline scripts to static files where practical.
5. Remove `unsafe-eval` from Content Security Policy.
6. Reduce `unsafe-inline` usage by using nonces or static assets.
7. Add `X-Content-Type-Options: nosniff`.

### Acceptance Criteria

- Tenant admin forms work with CSRF enabled.
- Cross-site form submissions to system tenant routes fail.
- CSP no longer allows `unsafe-eval`.

## Phase 6: JWT Authentication and Logout

Priority: High

### Tasks

1. Allow `JwtAuthFilter` to process `/api/v1/auth/logout`, or parse the bearer token manually inside logout.
2. Blacklist logout tokens until their actual JWT expiration.
3. Add cleanup for expired token blacklist rows.
4. Add tests for:
   - login
   - logout
   - use of logged-out token
   - expired token
   - blacklisted token
5. Consider refresh-token rotation before public API release.

### Acceptance Criteria

- Logout revokes the current bearer token.
- A logged-out token cannot access protected APIs.
- Blacklist expiration matches token expiration.

## Phase 7: File Upload and Download Hardening

Priority: High

### Tasks

1. Validate file type using magic bytes, not only request MIME type.
2. Normalize upload and download paths.
3. Enforce that resolved paths always stay under the configured upload root.
4. Sanitize original filenames before using them in headers.
5. Use Spring `ContentDisposition` builder for downloads.
6. Prefer `attachment` for untrusted uploads unless inline preview is required.
7. Add antivirus scanning hook if uploads become public-facing.
8. Add tests for:
   - MIME spoofing
   - path traversal
   - malformed filenames
   - over-limit uploads
   - cross-tenant file access

### Acceptance Criteria

- Uploads outside the allowed types are rejected.
- Download path traversal is impossible.
- Files cannot be resolved outside the upload directory.

## Phase 8: Deployment and Docker

Priority: High

### Tasks

1. Replace H2-based Docker Compose with PostgreSQL for production-like deployment.
2. Add a separate local/dev Compose file if H2 is still wanted for demos.
3. Ensure runtime dependencies match configured database.
4. Add health checks for:
   - application readiness
   - database connectivity
5. Add persistent volumes for:
   - PostgreSQL data
   - uploads
6. Run app as non-root user in container.
7. Pin image versions where practical.
8. Add backup/restore notes for database and uploads.

### Acceptance Criteria

- `docker compose up` starts app plus PostgreSQL successfully.
- Liquibase migrations run successfully against PostgreSQL.
- App health check fails if the database is unavailable.

## Phase 9: Database and Migration Readiness

Priority: High

### Tasks

1. Run Liquibase migrations against a clean PostgreSQL database.
2. Run migrations against a database with realistic existing data.
3. Verify row-level security policies.
4. Add indexes for common tenant-scoped queries.
5. Add indexes for common filters:
   - tenant ID
   - dates
   - statuses
   - foreign keys
6. Validate unique constraints are tenant-aware where required.
7. Add database backup strategy.

### Acceptance Criteria

- Clean PostgreSQL migration succeeds.
- Upgrade migration succeeds without data loss.
- Tenant data is isolated at the database layer.

## Phase 10: Observability and Operations

Priority: Medium

### Tasks

1. Add Spring Boot Actuator.
2. Expose only safe production endpoints.
3. Add structured logging.
4. Add request correlation IDs.
5. Log security events:
   - failed login
   - tenant mismatch
   - forbidden access
   - admin actions
6. Add operational dashboards for:
   - error rate
   - latency
   - database health
   - disk usage
   - upload storage usage
7. Add alerting for critical failures.

### Acceptance Criteria

- Operators can determine if the app is healthy.
- Security-relevant events are auditable.
- Logs do not contain passwords, JWTs, or sensitive payloads.

## Phase 11: Testing Expansion

Priority: High

### Tasks

1. Add PostgreSQL-backed integration tests using Testcontainers.
2. Keep H2 only for fast context or unit tests if useful.
3. Add MockMvc tests for:
   - authentication
   - CSRF
   - permissions
   - tenant isolation
   - file upload/download
4. Add service tests for:
   - finance aggregation
   - HR attendance report
   - storage quota calculation
5. Add regression tests for all critical fixes.
6. Reduce test log noise from PostgreSQL-specific RLS calls under H2.

### Acceptance Criteria

- Tests fail if a low-privilege user reaches a protected route.
- Tests fail if tenant isolation breaks.
- CI can run the full suite reliably.

## Phase 12: Performance Readiness

Priority: Medium

### Tasks

1. Enable production Thymeleaf caching.
2. Optimize HR attendance report queries.
3. Optimize finance dashboard monthly aggregates.
4. Replace repeated filesystem upload-size scans with stored tenant usage.
5. Replace system dashboard tenant user-count loops with grouped queries.
6. Add pagination to heavy lists and reports.
7. Add query timing logs in non-production performance testing.
8. Load test common workflows.

### Acceptance Criteria

- Dashboard and reports use bounded query counts.
- Upload validation does not slow down as file count grows.
- Basic load test meets agreed response-time targets.

## Phase 13: Release Process

Priority: Medium

### Tasks

1. Add CI pipeline:
   - compile
   - unit tests
   - integration tests
   - dependency scan
   - container build
2. Add dependency vulnerability scanning.
3. Add release checklist.
4. Add rollback procedure.
5. Add database backup before migration.
6. Add smoke tests after deployment.

### Acceptance Criteria

- Every production release has a repeatable build.
- Deployment can be rolled back.
- Critical smoke tests run after deployment.

## Suggested Implementation Order

1. Externalize secrets and add production profile.
2. Fix Docker/PostgreSQL deployment.
3. Disable tenant header fallback in production.
4. Re-enable CSRF for system tenant administration.
5. Fix JWT logout blacklisting.
6. Add explicit permissions to all business controllers.
7. Harden file upload/download handling.
8. Add PostgreSQL integration tests.
9. Add observability and health checks.
10. Complete performance optimizations.
11. Add CI, scanning, backup, and release process.

## Final Production Sign-Off Checklist

- [ ] No secrets committed.
- [ ] Production profile configured.
- [ ] Docker deployment uses PostgreSQL.
- [ ] Liquibase migrations verified on PostgreSQL.
- [ ] Tenant isolation verified.
- [ ] Permission checks applied to all sensitive routes.
- [ ] CSRF enabled for browser admin routes.
- [ ] JWT logout revokes tokens.
- [ ] File upload/download hardened.
- [ ] Integration tests pass.
- [ ] Dependency scan reviewed.
- [ ] Backup and restore process documented.
- [ ] Health checks and logs ready.
- [ ] Smoke test checklist complete.

