package com.levanto.flooring.controller;

import com.levanto.flooring.enums.*;
import com.levanto.flooring.service.ApprovalService;
import com.levanto.flooring.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

@Controller @RequestMapping("/admin") @RequiredArgsConstructor
public class DashboardController {

    private final EnquiryService    enquiryService;
    private final BillingService    billingService;
    private final InventoryService  inventoryService;
    private final ExpenseService    expenseService;
    private final ApprovalService   approvalService;
    private final ProjectService    projectService;

    @GetMapping({"/dashboard", ""})
    public String dashboard(Model m) {
        // Counts
        m.addAttribute("newEnquiries",     enquiryService.countNew());
        m.addAttribute("contactedEnquiries", enquiryService.countContacted());
        m.addAttribute("totalQuotations",  billingService.count(TransactionStatus.QUOTATION));
        m.addAttribute("totalBills",       billingService.count(TransactionStatus.FINAL_BILL));
        m.addAttribute("pendingPayments",  billingService.countPayment(PaymentStatus.PENDING));
        m.addAttribute("monthlyIncome",    billingService.monthlyIncome());
        m.addAttribute("pendingApprovals", approvalService.countPending());

        // Low stock alert (< 10 units)
        m.addAttribute("lowStockItems",    inventoryService.getLowStock(10));

        // This month expenses
        LocalDate from = LocalDate.now().withDayOfMonth(1);
        LocalDate to   = LocalDate.now();
        m.addAttribute("monthlyExpenses",  expenseService.totalForPeriod(from, to));

        // Recent data
        m.addAttribute("recentEnquiries",  enquiryService.getAll(0, 5, null, EnquiryStatus.NEW).getContent());
        m.addAttribute("recentBills",      billingService.getAll(0, 5, TransactionStatus.FINAL_BILL, null).getContent());

        // Project module stats
        m.addAttribute("activeProjects",   projectService.countActive());
        m.addAttribute("pendingApprovals", approvalService.countPending());

        return "admin/dashboard";
    }
}
