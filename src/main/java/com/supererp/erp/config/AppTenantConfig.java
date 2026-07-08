package com.supererp.erp.config;

import java.util.UUID;

/**
 * Application-level tenant identity.
 *
 * In single-tenant mode there is exactly one logical "tenant" — the application itself.
 * This fixed UUID is used wherever the schema still has a tenant_id column
 * (kept for schema compatibility) and wherever services previously read from TenantContext.
 *
 * The UUID is deterministic and must match the value seeded in the database by DataInitializer.
 */
public final class AppTenantConfig {

    private AppTenantConfig() {}

    /**
     * Fixed UUID for the single application tenant.
     * This value is seeded into the database and used everywhere tenant_id is needed.
     */
    public static final UUID APP_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
}
