package com.supererp.erp.tenant;

import com.supererp.erp.config.AppTenantConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for the single application tenant.
 * In single-tenant mode there is exactly one Tenant record with
 * id = AppTenantConfig.APP_TENANT_ID.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;

    /**
     * Returns the single application tenant record.
     */
    public Optional<Tenant> getApplicationTenant() {
        return tenantRepository.findById(AppTenantConfig.APP_TENANT_ID);
    }

    /**
     * Backward-compatible lookup by ID — always returns the application tenant.
     */
    public Optional<Tenant> findById(UUID id) {
        return tenantRepository.findById(AppTenantConfig.APP_TENANT_ID);
    }

    /**
     * Backward-compatible lookup by slug.
     */
    public Optional<Tenant> findBySlug(String slug) {
        return tenantRepository.findBySlugAndActiveTrue(slug);
    }

    /**
     * Returns the upload size in GB for the application.
     */
    @Transactional(readOnly = true)
    public double getUploadSizeInGB() {
        return getApplicationTenant()
                .map(t -> t.getUploadSizeBytes() != null
                        ? t.getUploadSizeBytes() / (1024.0 * 1024.0 * 1024.0) : 0.0)
                .orElse(0.0);
    }

    /**
     * Increments the tracked upload size for the application.
     */
    @Transactional
    public void incrementUploadSize(long bytesDelta) {
        tenantRepository.findById(AppTenantConfig.APP_TENANT_ID).ifPresent(t -> {
            Long current = t.getUploadSizeBytes();
            if (current == null) current = 0L;
            t.setUploadSizeBytes(Math.max(0, current + bytesDelta));
            tenantRepository.save(t);
        });
    }

    /**
     * Updates application-level settings that are stored in the tenant record.
     */
    @Transactional
    public Tenant updateApplicationSettings(Tenant updated) {
        Tenant existing = tenantRepository.findById(AppTenantConfig.APP_TENANT_ID)
                .orElseThrow(() -> new IllegalStateException("Application tenant record not found"));
        existing.setName(updated.getName());
        existing.setLogoUrl(updated.getLogoUrl());
        existing.setPrimaryColor(updated.getPrimaryColor());
        existing.setMaxStorageGb(updated.getMaxStorageGb());
        return tenantRepository.save(existing);
    }
}
