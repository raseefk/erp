package com.levanto.flooring.controller;

import com.levanto.flooring.entity.CompanySettings;
import com.levanto.flooring.service.CompanySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class CompanySettingsController {

    private final CompanySettingsService settingsService;

    @GetMapping
    public String viewSettings(Model model) {
        model.addAttribute("settings", settingsService.getSettings());
        return "settings/form";
    }

    @PostMapping
    public String saveSettings(
            @ModelAttribute CompanySettings settings, 
            @org.springframework.web.bind.annotation.RequestParam(required = false) java.util.List<String> offDays,
            RedirectAttributes redirectAttributes) {
        
        if (offDays != null && !offDays.isEmpty()) {
            settings.setWeeklyOffDays(String.join(",", offDays));
        } else {
            settings.setWeeklyOffDays("");
        }
        
        settingsService.updateSettings(settings);
        redirectAttributes.addFlashAttribute("successMessage", "Company settings updated successfully!");
        return "redirect:/settings";
    }
}
