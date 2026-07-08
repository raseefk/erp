package com.supererp.erp.config;

import com.supererp.erp.entity.CompanySettings;
import com.supererp.erp.service.CompanySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injects application-level branding attributes into every Thymeleaf view.
 * Reads company settings from the database so changes take effect immediately.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final CompanySettingsService companySettingsService;
    private final CompanyProperties      companyProperties;

    @ModelAttribute("companyName")
    public String getCompanyName() {
        try {
            CompanySettings s = companySettingsService.getSettings();
            return (s.getCompanyName() != null && !s.getCompanyName().isBlank())
                ? s.getCompanyName() : companyProperties.getName();
        } catch (Exception e) {
            return companyProperties.getName();
        }
    }

    @ModelAttribute("companyTagline")
    public String getCompanyTagline() {
        try {
            CompanySettings s = companySettingsService.getSettings();
            return (s.getTagline() != null && !s.getTagline().isBlank())
                ? s.getTagline() : companyProperties.getTagline();
        } catch (Exception e) {
            return companyProperties.getTagline();
        }
    }
}
