package com.supererp.erp.controller;

import com.supererp.erp.enums.*;
import com.supererp.erp.service.*;
import com.supererp.erp.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

/**
 * Main application dashboard — single-tenant mode.
 * Previously this was the "tenant" dashboard; it now serves as the primary
 * dashboard for all authenticated users.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@com.supererp.erp.rbac.annotation.RequiresFeature("SYSTEM")
public class DashboardController {

    private final EnquiryService     enquiryService;
    private final BillingService     billingService;
    private final InventoryService   inventoryService;
    private final ExpenseService     expenseService;
    private final ApprovalService    approvalService;
    private final ProjectService     projectService;
    private final FileStorageService fileStorageService;

    @GetMapping({"/dashboard", ""})
    public String dashboard(Model m, org.springframework.security.core.Authentication auth) {
        boolean canSeeDashboard = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                           a.getAuthority().equals("PERM_DASHBOARD_VIEW"));

        if (!canSeeDashboard) {
            return "redirect:/admin/home";
        }

        // ── KPI counts ────────────────────────────────────────────────────
        m.addAttribute("newEnquiries",       enquiryService.countNew());
        m.addAttribute("contactedEnquiries", enquiryService.countContacted());
        m.addAttribute("totalQuotations",    billingService.count(TransactionStatus.QUOTATION));
        m.addAttribute("totalBills",         billingService.count(TransactionStatus.FINAL_BILL));
        m.addAttribute("pendingPayments",    billingService.countPayment(PaymentStatus.PENDING));
        m.addAttribute("monthlyIncome",      billingService.monthlyIncome());
        m.addAttribute("totalIncome",        billingService.totalIncome());
        m.addAttribute("pendingApprovals",   approvalService.countPending());

        // ── Low stock alert ────────────────────────────────────────────────
        m.addAttribute("lowStockItems",      inventoryService.getLowStock(10));

        // ── This month expenses ────────────────────────────────────────────
        LocalDate from = LocalDate.now().withDayOfMonth(1);
        LocalDate to   = LocalDate.now();
        m.addAttribute("monthlyExpenses",    expenseService.totalForPeriod(from, to));

        // ── Recent data ────────────────────────────────────────────────────
        m.addAttribute("recentEnquiries",    enquiryService.getAll(0, 5, null, EnquiryStatus.NEW).getContent());
        m.addAttribute("recentBills",        billingService.getAll(0, 5, TransactionStatus.FINAL_BILL, null).getContent());

        // ── Project stats ──────────────────────────────────────────────────
        m.addAttribute("activeProjects",     projectService.countActive());

        // ── Storage usage ──────────────────────────────────────────────────
        double usedGb  = fileStorageService.getUploadSizeInGB();
        double maxGb   = 100.0;
        double storagePercent = maxGb > 0 ? (usedGb / maxGb) * 100.0 : 0;
        m.addAttribute("storageUsedGb",   usedGb);
        m.addAttribute("storageMaxGb",    maxGb);
        m.addAttribute("storagePercent",  storagePercent);

        return "admin/dashboard";
    }
}
