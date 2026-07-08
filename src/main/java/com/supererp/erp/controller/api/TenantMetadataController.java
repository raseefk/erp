package com.supererp.erp.controller.api;

import com.supererp.erp.config.CompanyProperties;
import com.supererp.erp.service.CompanySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public endpoint returning application branding metadata for the login page.
 * Single-tenant: always returns the one application's settings.
 *
 * Kept at the same URL (/api/v1/tenant/metadata) for frontend compatibility.
 */
@RestController
@RequestMapping("/api/v1/tenant")
@RequiredArgsConstructor
public class TenantMetadataController {

    private final CompanySettingsService companySettingsService;
    private final CompanyProperties      companyProperties;

    @GetMapping("/metadata")
    public ResponseEntity<?> getMetadata() {
        try {
            var settings = companySettingsService.getSettings();
            String name = (settings.getCompanyName() != null && !settings.getCompanyName().isBlank())
                ? settings.getCompanyName() : companyProperties.getName();
            String tagline = (settings.getTagline() != null && !settings.getTagline().isBlank())
                ? settings.getTagline() : companyProperties.getTagline();

            return ResponseEntity.ok(Map.of(
                "businessName",  name,
                "tagline",       tagline,
                "primaryColor",  "#3b82f6",
                "loginTitle",    "Sign in to " + name,
                "active",        true
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "businessName", companyProperties.getName(),
                "tagline",      companyProperties.getTagline(),
                "primaryColor", "#3b82f6",
                "loginTitle",   "Sign in to " + companyProperties.getName(),
                "active",       true
            ));
        }
    }
}
