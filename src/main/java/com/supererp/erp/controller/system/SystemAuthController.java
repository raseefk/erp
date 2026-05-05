package com.supererp.erp.controller.system;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SystemAuthController {

    @GetMapping("/system/login")
    public String systemLogin(@RequestParam(required = false) String error,
                               @RequestParam(required = false) String logout,
                               Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) {
            return "redirect:/system/tenants";
        }
        if (error != null) {
            model.addAttribute("errorMsg", "Invalid system credentials.");
        }
        if (logout != null) {
            model.addAttribute("logoutMsg", "System session ended.");
        }
        model.addAttribute("pageTitle", "System Administration Login");
        return "system/login";
    }
}
