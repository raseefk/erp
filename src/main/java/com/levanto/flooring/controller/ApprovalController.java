package com.levanto.flooring.controller;

import com.levanto.flooring.dto.ApiResponse;
import com.levanto.flooring.entity.AppUser;
import com.levanto.flooring.repository.AppUserRepository;
import com.levanto.flooring.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService          approvalService;
    private final ProjectAnalyticsService  analyticsService;
    private final ProjectService           projectService;
    private final AppUserRepository        userRepo;

    // ── Approval Queue Dashboard ──────────────────────────────────────────────
    @GetMapping
    public String queue(Model m) {
        m.addAttribute("pendingGrouped", approvalService.getPendingGroupedByProject());
        m.addAttribute("totalPending",   approvalService.countPending());
        return "approval/queue";
    }

    // ── Project Analytics (chart + pulse) ─────────────────────────────────────
    @GetMapping("/analytics/{projectId}")
    public String analytics(@PathVariable Long projectId,
                            @RequestParam(defaultValue = "30") int days,
                            Model m) {
        var project = projectService.getById(projectId);
        m.addAttribute("project",    project);
        m.addAttribute("chartData",  analyticsService.getDailyProfit(projectId, days));
        m.addAttribute("pulse",      analyticsService.getProjectPulse(
                                         projectId, project.getTotalContractValue()));
        m.addAttribute("days",       days);
        return "approval/analytics";
    }

    // ── Approve single expense (REST) ─────────────────────────────────────────
    @PostMapping("/approve/{id}") @ResponseBody
    public ResponseEntity<ApiResponse<?>> approve(@PathVariable Long id, Authentication auth) {
        try {
            approvalService.approve(id, resolveUser(auth));
            return ResponseEntity.ok(ApiResponse.ok("Expense approved and posted to company ledger."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Reject single expense (REST) ──────────────────────────────────────────
    @PostMapping("/reject/{id}") @ResponseBody
    public ResponseEntity<ApiResponse<?>> reject(@PathVariable Long id, Authentication auth) {
        try {
            approvalService.reject(id, resolveUser(auth));
            return ResponseEntity.ok(ApiResponse.ok("Expense rejected."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Bulk approve all NEW for a project ────────────────────────────────────
    @PostMapping("/approve-all/{projectId}")
    public String approveAll(@PathVariable Long projectId,
                             Authentication auth,
                             RedirectAttributes ra) {
        try {
            int count = approvalService.approveAllForProject(projectId, resolveUser(auth));
            ra.addFlashAttribute("success",
                count + " expense(s) approved and posted to company ledger.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Approval failed: " + e.getMessage());
        }
        return "redirect:/admin/approval";
    }

    // ── Badge count (used by sidebar) ─────────────────────────────────────────
    @GetMapping("/count") @ResponseBody
    public ResponseEntity<ApiResponse<?>> countPending() {
        return ResponseEntity.ok(ApiResponse.ok("ok", approvalService.countPending()));
    }


    private AppUser resolveUser(Authentication auth) {
        return userRepo.findByUsername(auth.getName()).orElse(null);
    }
}
