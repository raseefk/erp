package com.supererp.erp.controller.system;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirects old /system/login URL to the unified login page.
 * Kept to avoid 404 errors from bookmarks or cached links.
 */
@Controller
public class SystemAuthController {

    @GetMapping("/system/login")
    public String systemLogin(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            return "redirect:/admin/home";
        }
        return "redirect:/login";
    }
}
