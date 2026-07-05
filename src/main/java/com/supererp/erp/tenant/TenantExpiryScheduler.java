package com.supererp.erp.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled job that runs every day at midnight (00:00) to deactivate
 * tenants whose subscription / trial has expired.
 *
 * A tenant is deactivated when:
 *   - expiresAt is set (not null), AND
 *   - expiresAt is in the past, AND
 *   - the tenant is still marked active
 *
 * Once deactivated, the tenant's users cannot log in and subdomain returns 403.
 * The tenant data is preserved and can be reactivated by a system admin.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantExpiryScheduler {

    private final TenantRepository tenantRepository;

    /**
     * Runs every day at midnight UTC.
     * Cron: second  minute  hour  day  month  weekday
     *        0       0       0     *    *       *
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deactivateExpiredTenants() {
        OffsetDateTime now = OffsetDateTime.now();
        log.info("TenantExpiryScheduler: checking for expired tenants at {}", now);

        List<Tenant> expired = tenantRepository.findExpiredActiveTenants(now);

        if (expired.isEmpty()) {
            log.info("TenantExpiryScheduler: no expired tenants found.");
            return;
        }

        for (Tenant tenant : expired) {
            tenant.setActive(false);
            tenantRepository.save(tenant);
            log.warn("TenantExpiryScheduler: deactivated tenant '{}' (id={}, slug={}, expiredAt={})",
                    tenant.getName(), tenant.getId(), tenant.getSlug(), tenant.getExpiresAt());
        }

        log.info("TenantExpiryScheduler: deactivated {} expired tenant(s).", expired.size());
    }
}
