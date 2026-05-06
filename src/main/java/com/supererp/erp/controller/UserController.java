package com.supererp.erp.controller;

import com.supererp.erp.entity.AppUser;
import com.supererp.erp.rbac.service.RbacService;
import com.supererp.erp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RbacService rbacService;

    // ── Profile Management (Any Logged In User) ──────────────────────────────
    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        AppUser user = userService.getByUsername(auth.getName());
        model.addAttribute("user", user);
        return "user/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 RedirectAttributes ra) {
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match.");
            return "redirect:/admin/profile";
        }
        
        try {
            userService.changePassword(auth.getName(), currentPassword, newPassword);
            ra.addFlashAttribute("success", "Password updated successfully. Please log in with your new password next time.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/profile";
    }

    // ── User Management (Admin Only) ─────────────────────────────────────────
    @GetMapping("/users")
    @com.supererp.erp.rbac.annotation.RequiresFeature("ADMIN")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "user/list";
    }

    @GetMapping("/users/new")
    @com.supererp.erp.rbac.annotation.RequiresFeature("ADMIN")
    public String newUserForm(Model model) {
        model.addAttribute("user", new AppUser());
        model.addAttribute("roles", rbacService.getRolesForCurrentTenant());
        return "user/form";
    }

    @PostMapping("/users")
    @com.supererp.erp.rbac.annotation.RequiresFeature("ADMIN")
    public String createUser(@ModelAttribute AppUser user, 
                             @RequestParam Long roleId,
                             RedirectAttributes ra) {
        try {
            if (user.getRoles() == null) user.setRoles(new java.util.HashSet<>());
            rbacService.getRoleWithPermissions(roleId).ifPresent(role -> {
                user.getRoles().add(role);
            });
            userService.createUser(user);
            ra.addFlashAttribute("success", "User created successfully.");
            return "redirect:/admin/users";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error creating user: " + e.getMessage());
            return "redirect:/admin/users/new";
        }
    }
    
    @PostMapping("/users/{id}/toggle")
    @com.supererp.erp.rbac.annotation.RequiresFeature("ADMIN")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.toggleStatus(id);
            ra.addFlashAttribute("success", "User status updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}/edit")
    @com.supererp.erp.rbac.annotation.RequiresFeature("ADMIN")
    public String editUserForm(@PathVariable Long id, Model model) {
        AppUser user = userService.getById(id);
        model.addAttribute("user", user);
        model.addAttribute("roles", rbacService.getRolesForCurrentTenant());
        return "user/form";
    }

    @PostMapping("/users/{id}")
    @org.springframework.transaction.annotation.Transactional
    @com.supererp.erp.rbac.annotation.RequiresFeature("ADMIN")
    public String updateUser(@PathVariable Long id, 
                             @ModelAttribute AppUser userDetails,
                             @RequestParam(required = false) Long roleId,
                             @RequestParam(required = false) String newPassword,
                             RedirectAttributes ra) {
        try {
            userService.updateUser(id, userDetails, roleId, newPassword);
            ra.addFlashAttribute("success", "User updated successfully.");
            return "redirect:/admin/users";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error updating user: " + e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }
}
