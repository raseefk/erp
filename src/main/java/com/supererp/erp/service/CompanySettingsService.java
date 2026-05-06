package com.supererp.erp.service;

import com.supererp.erp.entity.CompanySettings;
import com.supererp.erp.repository.CompanySettingsRepository;
import com.supererp.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanySettingsService {

    private final CompanySettingsRepository repository;

    @Transactional
    public CompanySettings getSettings() {
        // If no tenant context (e.g. System Admin), return a transient default — never save to DB
        if (TenantContext.getTenantId() == null) {
            return CompanySettings.builder()
                    .companyName("Super ERP")
                    .tagline("Enterprise Resource Planning")
                    .build();
        }
        return repository.findByTenantId(TenantContext.getTenantId()).orElseGet(() -> {
            CompanySettings defaultSettings = CompanySettings.builder()
                    .tenantId(TenantContext.getTenantId())
                    .companyName("New Organization")
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
