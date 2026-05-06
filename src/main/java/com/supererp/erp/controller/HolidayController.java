package com.supererp.erp.controller;

import com.supererp.erp.entity.Holiday;
import com.supererp.erp.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/settings/holidays")
@RequiredArgsConstructor
@com.supererp.erp.rbac.annotation.RequiresFeature("SYSTEM")
public class HolidayController {

    private final HolidayRepository holidayRepository;
    private final com.supererp.erp.service.CompanySettingsService settingsService;

    @GetMapping
    public String viewHolidays(@RequestParam(required = false) Integer year, Model model) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        
        LocalDate start = LocalDate.of(targetYear, 1, 1);
        LocalDate end = LocalDate.of(targetYear, 12, 31);
        
        List<Holiday> holidays = holidayRepository.findByDateBetween(start, end);
        holidays.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        
        model.addAttribute("holidays", holidays);
        model.addAttribute("selectedYear", targetYear);
        model.addAttribute("years", java.util.Arrays.asList(targetYear - 1, targetYear, targetYear + 1));
        model.addAttribute("weeklyOffDays", settingsService.getSettings().getWeeklyOffDaysList());
        model.addAttribute("allDaysOfWeek", java.time.DayOfWeek.values());
        
        return "settings/holidays";
    }

    @PostMapping("/weekly-off")
    public String updateWeeklyOff(
            @RequestParam(required = false) List<String> offDays,
            RedirectAttributes redirectAttributes) {
        try {
            com.supererp.erp.entity.CompanySettings settings = settingsService.getSettings();
            if (offDays != null && !offDays.isEmpty()) {
                settings.setWeeklyOffDays(String.join(",", offDays));
            } else {
                settings.setWeeklyOffDays("");
            }
            settingsService.updateSettings(settings);
            redirectAttributes.addFlashAttribute("success", "Weekly off days updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update weekly off days.");
        }
        return "redirect:/settings/holidays";
    }

    @PostMapping("/add")
    public String addHoliday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String name,
            RedirectAttributes redirectAttributes) {
        try {
            if (holidayRepository.findByDate(date).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "A holiday already exists for this date.");
            } else {
                holidayRepository.save(Holiday.builder().date(date).name(name).build());
                redirectAttributes.addFlashAttribute("success", "Holiday added successfully.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add holiday.");
        }
        return "redirect:/settings/holidays?year=" + date.getYear();
    }

    @PostMapping("/{id}/delete")
    public String deleteHoliday(@PathVariable Long id, @RequestParam Integer year, RedirectAttributes redirectAttributes) {
        try {
            holidayRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Holiday removed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to remove holiday.");
        }
        return "redirect:/settings/holidays?year=" + year;
    }
}
