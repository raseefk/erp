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
    
    public static UUID getTenantId() {
        UUID id = TENANT_ID.get();
        if (id == null) {
            // Fallback: Try to resolve from SecurityContext if available
            try {
                org.springframework.security.core.Authentication auth = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null) {
                    if (auth.getPrincipal() instanceof com.supererp.erp.security.SecurityUser) {
                        return ((com.supererp.erp.security.SecurityUser) auth.getPrincipal()).getTenantId();
                    } else if (auth instanceof com.supererp.erp.security.jwt.JwtAuthToken) {
                        String tid = ((com.supererp.erp.security.jwt.JwtAuthToken) auth).getTenantId();
                        if (tid != null && !"SYSTEM".equals(tid)) {
                            return UUID.fromString(tid);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore if security context is not available
            }
        }
        return id;
    }

    public static void setTenantSlug(String slug){ TENANT_SLUG.set(slug); }
    public static String getTenantSlug()         { return TENANT_SLUG.get(); }

    public static boolean hasActiveTenant() { return TENANT_ID.get() != null; }

    public static void clear() {
        TENANT_ID.remove();
        TENANT_SLUG.remove();
    }
}
