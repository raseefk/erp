package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.entity.*;
import com.supererp.erp.rbac.Permissions;
import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.rbac.annotation.RequiresPermission;
import com.supererp.erp.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * Payroll & Compliance Engine controller.
 * Routes: /admin/payroll/**
 */
@Controller
@RequestMapping("/admin/payroll")
@RequiredArgsConstructor
@RequiresFeature("HR")
public class PayrollController {

    private final PayrollService             payrollService;
    private final PayslipPdfService          payslipPdfService;
    private final Form16PdfService           form16PdfService;
    private final EmployeeService            employeeService;
    private final StatutoryComplianceService complianceService;

    // ── Dashboard ──────────────────────────────────────────────────────────────
    @GetMapping
    @RequiresPermission(Permissions.HR_PAYROLL_VIEW)
    @Transactional(readOnly = true)
    public String dashboard(Model model) {
        model.addAttribute("metrics", payrollService.getDashboardMetrics());
        model.addAttribute("pendingArrears", payrollService.getUnpaidArrears());
        return "payroll/dashboard";
    }

    // ── Payroll Runs ───────────────────────────────────────────────────────────

    @GetMapping("/runs")
    @RequiresPermission(Permissions.HR_PAYROLL_VIEW)
    @Transactional(readOnly = true)
    public String runs(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("runsPage", payrollService.getAllRuns(page, 20));
        model.addAttribute("currentPage", page);
        model.addAttribute("months", java.time.Month.values());
        model.addAttribute("currentYear", LocalDate.now().getYear());
        model.addAttribute("currentMonth", LocalDate.now().getMonthValue());
        return "payroll/runs";
    }

    @PostMapping("/runs/generate")
    @RequiresPermission(Permissions.HR_PAYROLL_RUN)
    public String generateRun(@RequestParam int month, @RequestParam int year,
                               Principal principal, RedirectAttributes ra) {
        try {
            PayrollRun run = payrollService.generateRun(month, year,
                principal != null ? principal.getName() : "system");
            ra.addFlashAttribute("success",
                "Payroll generated for " + run.getPayPeriodLabel() +
                " — " + run.getEntries().size() + " employees.");
            return "redirect:/admin/payroll/runs/" + run.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/payroll/runs";
        }
    }

    @GetMapping("/runs/{id}")
    @RequiresPermission(Permissions.HR_PAYROLL_VIEW)
    @Transactional(readOnly = true)
    public String viewRun(@PathVariable Long id, Model model) {
        PayrollRun run = payrollService.getRunById(id);
        model.addAttribute("run", run);
        model.addAttribute("entries", payrollService.getRunEntries(id));
        model.addAttribute("activePage", "payroll-runs");
        return "payroll/run-detail";
    }

    @PostMapping("/runs/{id}/approve")
    @ResponseBody
    @RequiresPermission(Permissions.HR_PAYROLL_APPROVE)
    public ResponseEntity<ApiResponse<?>> approveRun(@PathVariable Long id, Principal principal) {
        try {
            payrollService.approveRun(id, principal != null ? principal.getName() : "system");
            return ResponseEntity.ok(ApiResponse.ok("Payroll run approved."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/runs/{id}/cancel")
    @ResponseBody
    @RequiresPermission(Permissions.HR_PAYROLL_APPROVE)
    public ResponseEntity<ApiResponse<?>> cancelRun(@PathVariable Long id) {
        try {
            payrollService.cancelRun(id);
            return ResponseEntity.ok(ApiResponse.ok("Payroll run cancelled."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── NEFT Bank Disbursement File ────────────────────────────────────────────
    @GetMapping("/runs/{id}/neft")
    @RequiresPermission(Permissions.HR_PAYROLL_DISBURSE)
    public ResponseEntity<byte[]> downloadNeft(@PathVariable Long id) {
        try {
            String csv = payrollService.generateNeftFile(id);
            PayrollRun run = payrollService.getRunById(id);
            String filename = "NEFT_" + run.getPayPeriodLabel().replace(" ", "_") + ".csv";
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ── Payslip PDF ───────────────────────────────────────────────────────────
    @GetMapping("/entries/{entryId}/payslip")
    @RequiresPermission(Permissions.HR_PAYROLL_VIEW)
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Long entryId) {
        try {
            PayrollEntry entry = payrollService.getEntry(entryId);
            byte[] pdf = payslipPdfService.generatePayslip(entry);
            String fn = "Payslip_" + entry.getEmployee().getName().replace(" ", "_")
                + "_" + entry.getPayrollRun().getPayPeriodLabel().replace(" ", "_") + ".pdf";
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fn + "\"")
                .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ── Form 16 ────────────────────────────────────────────────────────────────
    @GetMapping("/form16/{employeeId}/{fy}")
    @RequiresPermission(Permissions.HR_PAYROLL_FORM16)
    public ResponseEntity<byte[]> downloadForm16(@PathVariable Long employeeId,
                                                  @PathVariable int fy) {
        try {
            byte[] pdf = form16PdfService.generateForm16(employeeId, fy);
            String fn = "Form16_" + employeeId + "_FY" + fy + "-" + (fy + 1) + ".pdf";
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ── Payroll Config ────────────────────────────────────────────────────────
    @GetMapping("/config/{employeeId}")
    @RequiresPermission(Permissions.HR_PAYROLL_VIEW)
    @Transactional(readOnly = true)
    public String configForm(@PathVariable Long employeeId, Model model) {
        model.addAttribute("employee", employeeService.getById(employeeId));
        model.addAttribute("config", payrollService.getOrDefaultConfig(employeeId));
        return "payroll/config-form";
    }

    @PostMapping("/config/{employeeId}/save")
    @RequiresPermission(Permissions.HR_PAYROLL_RUN)
    public String saveConfig(@PathVariable Long employeeId,
                              @ModelAttribute PayrollConfig config,
                              RedirectAttributes ra) {
        try {
            payrollService.saveConfig(employeeId, config);
            ra.addFlashAttribute("success", "Payroll configuration saved.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/payroll/config/" + employeeId;
    }

    // ── Employee payroll history ───────────────────────────────────────────────
    @GetMapping("/employee/{employeeId}")
    @RequiresPermission(Permissions.HR_PAYROLL_VIEW)
    @Transactional(readOnly = true)
    public String employeeHistory(@PathVariable Long employeeId, Model model) {
        model.addAttribute("employee", employeeService.getById(employeeId));
        model.addAttribute("entries", payrollService.getEmployeeHistory(employeeId));
        model.addAttribute("arrears", payrollService.getEmployeeArrears(employeeId));
        model.addAttribute("currentYear", LocalDate.now().getYear());
        model.addAttribute("activePage", "payroll-runs");
        return "payroll/employee-history";
    }

    // ── Arrears management ────────────────────────────────────────────────────
    @GetMapping("/arrears")
    @RequiresPermission(Permissions.HR_PAYROLL_VIEW)
    @Transactional(readOnly = true)
    public String arrears(Model model) {
        model.addAttribute("arrears", payrollService.getUnpaidArrears());
        model.addAttribute("employees", employeeService.getActive());
        return "payroll/arrears";
    }

    @PostMapping("/arrears/save")
    @ResponseBody
    @RequiresPermission(Permissions.HR_PAYROLL_RUN)
    public ResponseEntity<ApiResponse<?>> saveArrear(
            @RequestParam Long employeeId,
            @RequestParam String period,
            @RequestParam(required = false) BigDecimal oldSalary,
            @RequestParam(required = false) BigDecimal newSalary,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String reason) {
        try {
            payrollService.createArrear(employeeId, period, oldSalary, newSalary, amount, reason);
            return ResponseEntity.ok(ApiResponse.ok("Arrear created."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Leave Encashment ──────────────────────────────────────────────────────
    @PostMapping("/leave-encashment/{year}")
    @ResponseBody
    @RequiresPermission(Permissions.HR_PAYROLL_RUN)
    public ResponseEntity<ApiResponse<?>> processLeaveEncashment(@PathVariable int year) {
        try {
            int count = payrollService.processLeaveEncashments(year);
            return ResponseEntity.ok(ApiResponse.ok("Leave encashment queued for " + count + " employees."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Statutory Compliance Hub & Exports ───────────────────────────────────
    @GetMapping("/compliance")
    @RequiresPermission(Permissions.HR_PAYROLL_VIEW)
    @Transactional(readOnly = true)
    public String complianceHub(Model model) {
        model.addAttribute("recentRuns", payrollService.getAllRuns(0, 12).getContent());
        model.addAttribute("currentYear", LocalDate.now().getYear());
        model.addAttribute("activePage", "payroll-compliance");
        return "payroll/compliance";
    }

    @GetMapping("/runs/{id}/pf-ecr")
    @RequiresPermission(Permissions.HR_PAYROLL_VIEW)
    public ResponseEntity<byte[]> downloadPfEcr(@PathVariable Long id) {
        try {
            String txt = complianceService.generatePfEcrFile(id);
            PayrollRun run = payrollService.getRunById(id);
            String filename = "PF_ECR_" + run.getPayPeriodLabel().replace(" ", "_") + ".txt";
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(txt.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/runs/{id}/esi-return")
    @RequiresPermission(Permissions.HR_PAYROLL_VIEW)
    public ResponseEntity<byte[]> downloadEsiReturn(@PathVariable Long id) {
        try {
            String csv = complianceService.generateEsiReturnCsv(id);
            PayrollRun run = payrollService.getRunById(id);
            String filename = "ESI_Return_" + run.getPayPeriodLabel().replace(" ", "_") + ".csv";
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/runs/{id}/pt-summary")
    @ResponseBody
    @RequiresPermission(Permissions.HR_PAYROLL_VIEW)
    public ResponseEntity<ApiResponse<?>> getPtSummary(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("PT Summary retrieved", complianceService.generatePtSummary(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
