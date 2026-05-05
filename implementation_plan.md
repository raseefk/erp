# Multi-Tenant ERP — FINAL Implementation Plan

## Decisions Locked

| Decision | Answer |
|---|---|
| Tenant ID Strategy | **Subdomain primary** (`acme.erp.com`), `X-Tenant-ID` header as fallback |
| Frontend | **Thymeleaf + JWT** + Thymeleaf fragments for conditional rendering |
| Cache | **Caffeine** (in-memory, Spring Cache abstraction — Redis-ready interface) |
| Existing Data | **Migrate to PostgreSQL** via Liquibase |
| RLS Level | **Tenant-level + User-level RLS** |
| Superuser | **SYSTEM_ADMIN** role — cross-tenant, manages tenants + subscriptions |

---

## Architecture

```
Multi-Tenant Stack:
  PostgreSQL (RLS: tenant_id + user context)
  Spring Boot 3.2 + Hibernate (Tenant Filter)
  JWT (stateless, tenant_id embedded)
  Caffeine (L1 cache — Redis interface for future swap)
  Thymeleaf (server-side, permission-aware fragments)
  Liquibase (schema + data migration)
```

---

## SYSTEM_ADMIN Superuser

- Lives outside any tenant (`tenant_id = NULL` / special system tenant)
- Has a separate login portal: `/system/login`
- Can: Create/Edit/Delete Tenants, Manage Subscriptions, View all audit logs
- Stored in `system_users` table (completely separate from `app_users`)
- JWT claims: `{ role: "SYSTEM_ADMIN", tenant_id: "SYSTEM" }`

---

## Phase 1: Infrastructure & Database

### 1.1 pom.xml changes
- Remove: H2
- Add: PostgreSQL, JJWT (0.12.x), Caffeine, Liquibase, Jackson (for JSONB)

### 1.2 Liquibase Migrations
- `V1__multi_tenant_foundation.sql` — tenants, system_users, features, menus, permissions, roles, user-roles, tenant-feature-mapping, audit_log, token_blacklist
- `V2__add_tenant_id_to_existing.sql` — Add tenant_id to all 24 existing entity tables
- `V3__rls_policies.sql` — Enable RLS on all tables, create policies for tenant + user isolation
- `V4__migrate_h2_data.sql` — Seed default "SYSTEM" tenant, migrate existing data under a "demo" tenant
- `V5__seed_permissions.sql` — Insert all feature/menu/permission records

### 1.3 Entity Layer
- New `TenantAwareEntity` base class with `@Filter` (Hibernate tenant filter)
- All 24 existing entities extend it
- New entities: `Tenant`, `TenantSettings`, `Feature`, `Menu`, `Permission`, `AppRole`, `UserRole`, `TenantFeatureMapping`, `AuditLog`, `TokenBlacklist`, `SystemUser`
- `AppUser` refactored: remove `Role` enum, add `tenantId`, add `UserRole` relationship

---

## Phase 2: Security

### 2.1 TenantContext
- `TenantContext.java` — ThreadLocal<UUID>
- `TenantResolutionFilter.java` — Order(-100), extracts subdomain
- Sets PostgreSQL session var via `EntityManager.createNativeQuery("SET LOCAL app.current_tenant_id = ?")`

### 2.2 JWT
- `JwtTokenProvider.java` — sign/verify HS256, embed tenant_id + user_id + permissions
- `JwtAuthFilter.java` — Order(-90), validates, breach-detects, sets SecurityContext
- `TokenBlacklistService.java` — Caffeine + DB persistence

### 2.3 Security Config
- Stateless session (STATELESS SessionCreationPolicy)
- JwtAuthFilter before UsernamePasswordAuthenticationFilter
- SYSTEM_ADMIN paths: `/system/**` — separate filter chain
- Public paths: `/api/v1/tenant/metadata`, `/login`, `/api/v1/auth/**`

### 2.4 Hibernate Tenant Filter Activation
- `HibernateTenantFilterAspect.java` — `@Around` all Repository calls, enables filter

---

## Phase 3: RBAC

### 3.1 Permissions.java (constants)
~50 permissions across 7 features: BILLING, HR, PROJECTS, INVENTORY, SCM, FINANCE, CRM, SETTINGS

### 3.2 Services
- `RbacService.java` — CRUD for roles, assign permissions
- `PermissionManifestBuilder.java` — Builds the JSON tree for login response
- `FeatureGuardService.java` — Checks tenant feature toggles (Caffeine cached)

### 3.3 Annotation + AOP
- `@RequiresPermission("BILLING_CREATE")` — method-level annotation
- `@RequiresFeature("BILLING")` — controller-level annotation
- `PermissionCheckAspect.java` + `FeatureGuardAspect.java`

### 3.4 Role-Permission Management UI
- `GET /settings/roles` — list roles with create/edit
- `GET /settings/roles/{id}/permissions` — feature-grouped checkbox UI
- `POST /api/v1/settings/roles/{id}/permissions` — save assignment
- Thymeleaf templates with checkboxes grouped by Feature → Menu → Permission

### 3.5 Tenant Management UI (SYSTEM_ADMIN only)
- `GET /system/tenants` — list all tenants
- `GET /system/tenants/new` + `POST` — create tenant (name, slug, plan, branding)
- `GET /system/tenants/{id}/edit` + `POST` — edit + manage subscription

---

## Phase 4: Cross-Cutting

### 4.1 Audit Trail
- `@AuditAction` annotation on service methods
- `AuditAspect.java` — SHA-256 hash of old state, persist `AuditLog`

### 4.2 Caffeine Cache
- `CacheConfig.java` — `@Bean CacheManager` (Caffeine, Spring Cache abstraction)
- Keys: `tenant-meta`, `tenant-features`, `permission-manifest`, `token-blacklist`
- TTLs: 10min for tenant, 5min for permissions, token blacklist until expiry

### 4.3 Public Metadata Endpoint
- `GET /api/v1/tenant/metadata?origin=https://acme.erp.com`

### 4.4 Thymeleaf Permission Fragments
- `fragments/rbac.html` — `th:if` macros checking permission manifest stored in session
- Layout sidebar driven by manifest
- Buttons with `data-permission="BILLING_CREATE"` hidden via JS if not in manifest
