package com.supererp.erp.controller.settings;

import com.supererp.erp.rbac.entity.AppRole;
import com.supererp.erp.rbac.entity.Feature;
import com.supererp.erp.rbac.repository.FeatureRepository;
import com.supererp.erp.rbac.service.RbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/settings/roles")
@RequiredArgsConstructor
public class RoleManagementController {

    private final RbacService       rbacService;
    private final FeatureRepository featureRepo;

    // ── Role List ─────────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_SETTINGS_ROLES_VIEW')")
    public String listRoles(Model model) {
        model.addAttribute("roles", rbacService.getRolesForCurrentTenant());
        model.addAttribute("pageTitle", "Roles & Permissions");
        return "settings/roles";
    }

    // ── Create Role Form ──────────────────────────────────────────────────────
    @GetMapping("/new")
    @PreAuthorize("hasAuthority('PERM_SETTINGS_ROLES_MANAGE')")
    public String newRoleForm(Model model) {
        model.addAttribute("pageTitle", "Create Role");
        return "settings/role-form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('PERM_SETTINGS_ROLES_MANAGE')")
    public String createRole(@RequestParam String name,
                              @RequestParam(required = false) String description,
                              RedirectAttributes ra) {
        try {
            rbacService.createRole(name, description);
            ra.addFlashAttribute("success", "Role '" + name + "' created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings/roles";
    }

    // ── Permission Assignment ─────────────────────────────────────────────────
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('PERM_SETTINGS_ROLES_VIEW')")
    public String permissionsPage(@PathVariable Long id, Model model) {
        AppRole role = rbacService.getRoleWithPermissions(id)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        Set<String> assigned = new HashSet<>();
        role.getPermissions().forEach(p -> assigned.add(p.getId()));

        List<Feature> features = featureRepo.findAllOrdered();

        model.addAttribute("role",           role);
        model.addAttribute("assignedPerms",  assigned);
        model.addAttribute("features",       features);
        model.addAttribute("pageTitle",      "Permissions — " + role.getName());
        return "settings/role-permissions";
    }

    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('PERM_SETTINGS_ROLES_MANAGE')")
    public String savePermissions(@PathVariable Long id,
                                   @RequestParam(value = "permissions", required = false)
                                   Set<String> permissions,
                                   RedirectAttributes ra) {
        rbacService.updateRolePermissions(id, permissions != null ? permissions : Set.of());
        ra.addFlashAttribute("success", "Permissions updated successfully.");
        return "redirect:/settings/roles/" + id + "/permissions";
    }

    // ── Delete Role ───────────────────────────────────────────────────────────
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('PERM_SETTINGS_ROLES_MANAGE')")
    public String deleteRole(@PathVariable Long id, RedirectAttributes ra) {
        try {
            rbacService.deleteRole(id);
            ra.addFlashAttribute("success", "Role deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings/roles";
    }
}
