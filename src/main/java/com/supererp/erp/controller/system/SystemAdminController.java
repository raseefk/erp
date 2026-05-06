package com.supererp.erp.controller.system;

import com.supererp.erp.rbac.annotation.AuditAction;
import com.supererp.erp.tenant.Tenant;
import com.supererp.erp.tenant.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN')")
public class SystemAdminController {

    private final TenantService tenantService;
    private final com.supererp.erp.rbac.service.RbacService rbacService;
    private final com.supererp.erp.rbac.repository.FeatureRepository featureRepo;
    private final com.supererp.erp.rbac.repository.MenuRepository menuRepo;
    private final jakarta.persistence.EntityManager entityManager;
    private final com.supererp.erp.service.FileStorageService fileStorageService;
    
    @InitBinder
    public void initBinder(org.springframework.web.bind.WebDataBinder binder) {
        binder.registerCustomEditor(java.time.OffsetDateTime.class, new java.beans.PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                } else {
                    try {
                        // datetime-local format: yyyy-MM-dd'T'HH:mm
                        java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(text);
                        setValue(ldt.atOffset(java.time.ZoneOffset.UTC));
                    } catch (Exception e) {
                        // Fallback to standard OffsetDateTime parsing
                        setValue(java.time.OffsetDateTime.parse(text));
                    }
                }
            }
        });
    }

    // ── System Dashboard ───────────────────────────────────────────────────
    @GetMapping({"", "/dashboard", "/"})
    public String systemDashboard(Model model) {
        List<Tenant> tenants = tenantService.findAll();
        
        // 1. OS Metrics
        com.sun.management.OperatingSystemMXBean osBean = 
            (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = osBean.getSystemCpuLoad() * 100.0;
        if (cpuLoad < 0) cpuLoad = 0.0; // Can be negative on first call
        double totalRamGb = osBean.getTotalMemorySize() / (1024.0 * 1024.0 * 1024.0);
        double freeRamGb = osBean.getFreeMemorySize() / (1024.0 * 1024.0 * 1024.0);
        double usedRamGb = totalRamGb - freeRamGb;
        double ramPercent = (usedRamGb / totalRamGb) * 100.0;

        // 2. Global DB Size
        Number dbSizeBytes = (Number) entityManager.createNativeQuery("SELECT pg_database_size(current_database())").getSingleResult();
        double dbSizeGb = dbSizeBytes.longValue() / (1024.0 * 1024.0 * 1024.0);

        // 3. Tenant Stats (Uploads and Est Rows)
        List<java.util.Map<String, Object>> tenantStats = tenants.stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .map(t -> {
                double uploadSize = fileStorageService.getTenantUploadSizeInGB(t.getId());
                // Simple row count estimation for UI
                Number rows = (Number) entityManager.createNativeQuery("SELECT count(*) FROM app_users WHERE tenant_id = :id")
                        .setParameter("id", t.getId()).getSingleResult();
                return java.util.Map.<String, Object>of(
                    "tenant", t,
                    "uploadSizeGb", uploadSize,
                    "activeUsers", rows.longValue()
                );
            }).toList();

        model.addAttribute("totalTenants", tenants.size());
        model.addAttribute("activeTenants", tenants.stream().filter(Tenant::isActive).count());
        model.addAttribute("tenantStats", tenantStats);
        model.addAttribute("cpuLoad", String.format("%.1f", cpuLoad));
        model.addAttribute("usedRamGb", String.format("%.1f", usedRamGb));
        model.addAttribute("totalRamGb", String.format("%.1f", totalRamGb));
        model.addAttribute("ramPercent", String.format("%.1f", ramPercent));
        model.addAttribute("dbSizeGb", String.format("%.3f", dbSizeGb));
        model.addAttribute("pageTitle", "System Health Dashboard");
        return "system/dashboard";
    }

    // ── Tenant List ─────────────────────────────────────────────────────────
    @GetMapping("/tenants")
    public String listTenants(Model model) {
        model.addAttribute("tenants", tenantService.findAll());
        model.addAttribute("pageTitle", "Tenant Management");
        return "system/tenants";
    }

    // ── New Tenant Form ──────────────────────────────────────────────────────
    @GetMapping("/tenants/new")
    public String newTenantForm(Model model) {
        model.addAttribute("tenant", Tenant.builder().build());
        model.addAttribute("plans", new String[]{"TRIAL", "STANDARD", "ENTERPRISE"});
        model.addAttribute("pageTitle", "Create New Tenant");
        model.addAttribute("isNew", true);
        return "system/tenant-form";
    }

    @PostMapping("/tenants/new")
    @AuditAction(value = "TENANT_CREATE", entityType = "Tenant")
    public String createTenant(@ModelAttribute Tenant tenant,
                                @RequestParam String adminUsername,
                                @RequestParam String adminPassword,
                                @RequestParam String adminFullName,
                                @RequestParam String adminEmail,
                                RedirectAttributes ra) {
        try {
            tenantService.createWithAdmin(tenant, adminUsername, adminPassword, adminFullName, adminEmail);
            ra.addFlashAttribute("success", "Tenant '" + tenant.getName() + "' and Administrator account created successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/system/tenants/new";
        }
        return "redirect:/system/tenants";
    }

    // ── Edit Tenant Form ─────────────────────────────────────────────────────
    @GetMapping("/tenants/{id}/edit")
    public String editTenantForm(@PathVariable UUID id, Model model) {
        Tenant tenant = tenantService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
        model.addAttribute("tenant", tenant);
        model.addAttribute("plans", new String[]{"TRIAL", "STANDARD", "ENTERPRISE"});
        model.addAttribute("pageTitle", "Edit Tenant — " + tenant.getName());
        model.addAttribute("isNew", false);

        // Feature & Menu toggling data
        model.addAttribute("allFeatures", featureRepo.findAllWithMenus());
        model.addAttribute("enabledFeatures", rbacService.getEnabledFeatures(id));
        
        // Disabled menus: find all menu mappings for this tenant that have enabled=false
        List<String> disabledMenuIds = rbacService.getMenuMappingsForTenant(id).stream()
                .filter(m -> !m.isEnabled())
                .map(m -> m.getMenuId())
                .toList();
        model.addAttribute("disabledMenuIds", disabledMenuIds);

        return "system/tenant-form";
    }

    @PostMapping("/tenants/{id}/edit")
    @AuditAction(value = "TENANT_UPDATE", entityType = "Tenant")
    public String updateTenant(@PathVariable UUID id,
                                @ModelAttribute Tenant tenant,
                                RedirectAttributes ra) {
        try {
            tenantService.update(id, tenant);
            ra.addFlashAttribute("success", "Tenant updated successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/system/tenants";
    }

    // ── Update Tenant Features & Menus ──────────────────────────────────────
    @PostMapping("/tenants/{id}/features")
    @AuditAction(value = "TENANT_FEATURE_UPDATE", entityType = "Tenant")
    public String updateTenantFeatures(@PathVariable UUID id,
                                       @RequestParam(name = "featureIds", required = false) List<String> featureIds,
                                       @RequestParam(name = "menuIds", required = false) List<String> menuIds,
                                       RedirectAttributes ra) {
        try {
            List<String> enabledFeatIds = featureIds != null ? featureIds : List.of();
            List<String> enabledMenuIds = menuIds != null ? menuIds : List.of();
            
            // 1. Update Features
            List<String> allFeatIds = featureRepo.findAll().stream().map(f -> f.getId()).toList();
            for (String fid : allFeatIds) {
                rbacService.toggleFeature(id, fid, enabledFeatIds.contains(fid));
            }

            // 2. Update Menus (Granular control)
            List<String> allMenuIds = menuRepo.findAll().stream().map(m -> m.getId()).toList();
            for (String mid : allMenuIds) {
                // If mid is in enabledMenuIds, toggle(enabled=true), else toggle(enabled=false)
                rbacService.toggleMenu(id, mid, enabledMenuIds.contains(mid));
            }

            ra.addFlashAttribute("success", "Feature and Menu permissions updated for tenant.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/system/tenants/" + id + "/edit";
    }

    // ── Deactivate Tenant ────────────────────────────────────────────────────
    @PostMapping("/tenants/{id}/deactivate")
    @AuditAction(value = "TENANT_DEACTIVATE", entityType = "Tenant")
    public String deactivateTenant(@PathVariable UUID id, RedirectAttributes ra) {
        tenantService.deactivate(id);
        ra.addFlashAttribute("success", "Tenant deactivated.");
        return "redirect:/system/tenants";
    }
}
