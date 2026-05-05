package com.supererp.erp.tenant;

import java.util.UUID;

/**
 * ThreadLocal holder for the current tenant's UUID.
 * Set by TenantResolutionFilter before any business logic executes.
 * Cleared in a finally block after the request completes.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID>   TENANT_ID   = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_SLUG = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(UUID id)     { TENANT_ID.set(id); }
    public static UUID  getTenantId()            { return TENANT_ID.get(); }

    public static void setTenantSlug(String slug){ TENANT_SLUG.set(slug); }
    public static String getTenantSlug()         { return TENANT_SLUG.get(); }

    public static boolean hasActiveTenant() { return TENANT_ID.get() != null; }

    public static void clear() {
        TENANT_ID.remove();
        TENANT_SLUG.remove();
    }
}
