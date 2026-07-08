package com.supererp.erp.tenant;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Removed in single-tenant conversion.
 * Tenant expiry scheduling is no longer needed — there is only one application instance.
 * Stub retained to avoid breaking bean dependencies in SystemAdminController until
 * that controller is also refactored.
 */
@Component
public class TenantExpiryScheduler {

    public ExpiryCheckResult runExpiryCheck() {
        return new ExpiryCheckResult(0, "Single-tenant mode — expiry check not applicable.");
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExpiryCheckResult {
        private int deactivatedCount;
        private String message;
    }
}
