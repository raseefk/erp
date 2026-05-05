package com.supererp.erp.controller.system;

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
import java.util.UUID;

@Controller
@RequestMapping("/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN')")
public class SystemAdminController {

    private final TenantService tenantService;

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
    public String createTenant(@ModelAttribute Tenant tenant,
                                RedirectAttributes ra) {
        try {
            tenantService.create(tenant);
            ra.addFlashAttribute("success", "Tenant '" + tenant.getName() + "' created successfully!");
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
        return "system/tenant-form";
    }

    @PostMapping("/tenants/{id}/edit")
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

    // ── Deactivate Tenant ────────────────────────────────────────────────────
    @PostMapping("/tenants/{id}/deactivate")
    public String deactivateTenant(@PathVariable UUID id, RedirectAttributes ra) {
        tenantService.deactivate(id);
        ra.addFlashAttribute("success", "Tenant deactivated.");
        return "redirect:/system/tenants";
    }
}
