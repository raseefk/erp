package com.levanto.flooring.service;

import com.levanto.flooring.entity.CompanySettings;
import com.levanto.flooring.repository.CompanySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanySettingsService {

    private final CompanySettingsRepository repository;

    public CompanySettings getSettings() {
        return repository.findAll().stream().findFirst().orElseGet(() -> {
            CompanySettings defaultSettings = CompanySettings.builder()
                    .companyName("Levanto Flooring")
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
