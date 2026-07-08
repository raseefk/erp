package com.supererp.erp.service;

import com.supererp.erp.config.AppTenantConfig;
import com.supererp.erp.entity.CompanySettings;
import com.supererp.erp.repository.CompanySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing application-level company settings.
 * There is exactly one CompanySettings record for the whole application.
 */
@Service
@RequiredArgsConstructor
public class CompanySettingsService {

    private final CompanySettingsRepository repository;

    @Transactional
    public CompanySettings getSettings() {
        return repository.findByTenantId(AppTenantConfig.APP_TENANT_ID).orElseGet(() -> {
            CompanySettings defaultSettings = CompanySettings.builder()
                    .tenantId(AppTenantConfig.APP_TENANT_ID)
                    .companyName("Super ERP")
                    .tagline("Enterprise Resource Planning Simplified")
                    .defaultSickLeavesPerYear(10)
                    .defaultCasualLeavesPerYear(10)
                    .build();
            return repository.save(defaultSettings);
        });
    }

    @Transactional
    public CompanySettings updateSettings(CompanySettings settings) {
        CompanySettings existing = getSettings();
        existing.setCompanyName(settings.getCompanyName());
        existing.setTagline(settings.getTagline());
        existing.setAddress(settings.getAddress());
        existing.setPhone(settings.getPhone());
        existing.setEmail(settings.getEmail());
        existing.setWebsite(settings.getWebsite());
        existing.setTaxNumber(settings.getTaxNumber());
        existing.setDefaultSickLeavesPerYear(settings.getDefaultSickLeavesPerYear());
        existing.setDefaultCasualLeavesPerYear(settings.getDefaultCasualLeavesPerYear());
        existing.setWeeklyOffDays(settings.getWeeklyOffDays());
        return repository.save(existing);
    }
}
