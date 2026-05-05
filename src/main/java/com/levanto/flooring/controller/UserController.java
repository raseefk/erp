package com.levanto.flooring.controller;

import com.levanto.flooring.entity.AppUser;
import com.levanto.flooring.enums.Role;
import com.levanto.flooring.service.UserService;
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
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "user/list";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new AppUser());
        model.addAttribute("roles", Role.values());
        return "user/form";
    }

    @PostMapping("/users")
    public String createUser(@ModelAttribute AppUser user, RedirectAttributes ra) {
        try {
            userService.createUser(user);
            ra.addFlashAttribute("success", "User created successfully.");
            return "redirect:/admin/users";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error creating user: " + e.getMessage());
            return "redirect:/admin/users/new";
        }
    }
    
    @PostMapping("/users/{id}/toggle")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.toggleStatus(id);
            ra.addFlashAttribute("success", "User status updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
