package com.levanto.flooring.controller;

import com.levanto.flooring.dto.ApiResponse;
import com.levanto.flooring.entity.*;
import com.levanto.flooring.enums.ProjectExpenseCategory;
import com.levanto.flooring.repository.AppUserRepository;
import com.levanto.flooring.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/sitelogs")
@RequiredArgsConstructor
public class DailyLogController {

    private final DailyLogService   logService;
    private final ProjectService    projectService;
    private final AppUserRepository userRepo;

    // ── Top-level log list (across all projects) ─────────────────────────────
    @GetMapping
    public String allLogs(@RequestParam(defaultValue="0") int page,
                          @RequestParam(defaultValue="20") int size,
                          Model m) {
        m.addAttribute("projects", projectService.getActive());
        return "sitelog/all";
    }

    // ── New log form ──────────────────────────────────────────────────────────
    @GetMapping("/new")
    public String newForm(@RequestParam Long jobCardId, Model m) {
        JobCard jc = projectService.getJobCard(jobCardId);
        DailyLog log = new DailyLog();
        log.setJobCard(jc);
        m.addAttribute("log",        log);
        m.addAttribute("jobCard",    jc);
        m.addAttribute("project",    jc.getProject());
        m.addAttribute("projectLabours", projectService.getProjectLabours(jc.getProject().getId()));
        m.addAttribute("miscCats",   new ProjectExpenseCategory[]{
            ProjectExpenseCategory.FUEL,
            ProjectExpenseCategory.TOOLS,
            ProjectExpenseCategory.TRANSPORT,
            ProjectExpenseCategory.MATERIAL,
            ProjectExpenseCategory.MISC,
            ProjectExpenseCategory.UTILITY,
            ProjectExpenseCategory.RENT
        });
        return "sitelog/form";
    }

    // ── Save log (multipart for optional misc expense files) ─────────────────
    @PostMapping(value = "/save", consumes = "multipart/form-data")
    public String save(@ModelAttribute DailyLog log,
                       @RequestParam Long jobCardId,
                       @RequestParam(required = false) List<Long> selectedLabourIds,
                       HttpServletRequest request,
                       Authentication auth,
                       RedirectAttributes ra) {
        JobCard jc = projectService.getJobCard(jobCardId);
        log.setJobCard(jc);
        
        if (selectedLabourIds != null && !selectedLabourIds.isEmpty()) {
            for (Long labId : selectedLabourIds) {
                String wageStr = request.getParameter("wage_" + labId);
                if (wageStr != null && !wageStr.trim().isEmpty()) {
                    BigDecimal wage = new BigDecimal(wageStr.trim());
                    DailyLabourLog dll = new DailyLabourLog();
                    dll.setProjectLabour(projectService.getProjectLabour(labId));
                    dll.setWagePaid(wage);
                    dll.setDailyLog(log);
                    log.getLabourLogs().add(dll);
                }
            }
        }
        
        AppUser user = resolveUser(auth);
        log.setLoggedBy(user);
        DailyLog saved = logService.saveLog(log, user);
        ra.addFlashAttribute("success",
            "Daily log saved. Labour expense auto-queued for approval.");
        return "redirect:/admin/sitelogs/" + saved.getId();
    }

    // ── View log detail with expenses ────────────────────────────────────────
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model m) {
        DailyLog log = logService.getById(id);
        m.addAttribute("log",      log);
        m.addAttribute("jobCard",  log.getJobCard());
        m.addAttribute("project",  log.getJobCard().getProject());
        m.addAttribute("expenses", logService.getExpensesForLog(id));
        m.addAttribute("miscCats", new ProjectExpenseCategory[]{
            ProjectExpenseCategory.FUEL,
            ProjectExpenseCategory.TOOLS,
            ProjectExpenseCategory.TRANSPORT,
            ProjectExpenseCategory.MATERIAL,
            ProjectExpenseCategory.MISC,
            ProjectExpenseCategory.UTILITY,
            ProjectExpenseCategory.RENT
        });
        return "sitelog/view";
    }

    // ── All logs overview ──────────────────────────────────────────────────────
    @GetMapping("/all")
    public String allLogsOverview(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  Model m) {
        m.addAttribute("projectList", projectService.getActive());
        return "sitelog/all";
    }

    // ── List logs for a job card ──────────────────────────────────────────────
    @GetMapping("/jobcard/{jobCardId}")
    public String byJobCard(@PathVariable Long jobCardId, Model m) {
        JobCard jc = projectService.getJobCard(jobCardId);
        m.addAttribute("logs",    logService.getByJobCard(jobCardId));
        m.addAttribute("jobCard", jc);
        m.addAttribute("project", jc.getProject());
        return "sitelog/list";
    }

    // ── Add misc expense to existing log (REST + file) ────────────────────────
    @PostMapping("/{id}/misc")
    public String addMisc(@PathVariable Long id,
                          @RequestParam String category,
                          @RequestParam String description,
                          @RequestParam BigDecimal amount,
                          @RequestParam(required = false) MultipartFile file,
                          Authentication auth,
                          RedirectAttributes ra) {
        try {
            ProjectExpenseCategory cat = ProjectExpenseCategory.valueOf(category);
            logService.addMiscExpense(id, cat, description, amount, file, resolveUser(auth));
            ra.addFlashAttribute("success", "Expense added to approval queue.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/sitelogs/" + id;
    }

    // ── Delete misc expense (only NEW) ───────────────────────────────────────
    @DeleteMapping("/expenses/{expId}") @ResponseBody
    public ResponseEntity<ApiResponse<?>> deleteMisc(@PathVariable Long expId) {
        try {
            logService.deleteMiscExpense(expId);
            return ResponseEntity.ok(ApiResponse.ok("Expense removed."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Delete log ────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}") @ResponseBody
    public ResponseEntity<ApiResponse<?>> deleteLog(@PathVariable Long id) {
        try {
            logService.deleteLog(id);
            return ResponseEntity.ok(ApiResponse.ok("Log deleted."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private AppUser resolveUser(Authentication auth) {
        return userRepo.findByUsername(auth.getName()).orElse(null);
    }
}
