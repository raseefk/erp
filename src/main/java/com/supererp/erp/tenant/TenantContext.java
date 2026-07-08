package com.supererp.erp.tenant;

/**
 * Stub retained for backward compatibility.
 * This application is single-tenant — there is no tenant context.
 * All methods are no-ops or return null.
 *
 * @deprecated This class will be removed in a future cleanup pass.
 */
@Deprecated(forRemoval = true)
public final class TenantContext {

    private TenantContext() {}

    /** Always returns null — single-tenant, no tenant ID concept. */
    public static java.util.UUID getTenantId() { return null; }

    /** No-op. */
    public static void setTenantId(java.util.UUID id) {}

    /** Always returns null. */
    public static String getTenantSlug() { return null; }

    /** No-op. */
    public static void setTenantSlug(String slug) {}

    /** Always returns false. */
    public static boolean hasActiveTenant() { return false; }

    /** No-op. */
    public static void clear() {}
}
