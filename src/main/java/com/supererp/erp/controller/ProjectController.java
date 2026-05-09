package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.entity.*;
import com.supererp.erp.enums.*;
import com.supererp.erp.repository.AppUserRepository;
import com.supererp.erp.service.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/projects")
@RequiredArgsConstructor
@com.supererp.erp.rbac.annotation.RequiresFeature("PROJECTS")
public class ProjectController {

    private final ProjectService       projectService;
    private final AppUserRepository    userRepo;
    private final com.supererp.erp.repository.EmployeeRepository employeeRepo;
    private final LabourWagePdfService labourPdfService;
    private final AdvancePaymentService advanceService;
    private final com.supererp.erp.repository.TransactionRepository transactionRepo;
    private final PaymentService paymentService;
    private final PdfService pdfService;

    // ── Project list ─────────────────────────────────────────────────────────
    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String search,
                       Model m) {
        m.addAttribute("projectsPage", projectService.getAll(page, size, search));
        m.addAttribute("search",       search);
        m.addAttribute("currentPage",  page);
        m.addAttribute("ProjectStatus", ProjectStatus.values());
        return "project/list";
    }

    // ── Project detail + analytics ────────────────────────────────────────────
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model m) {
        Project p = projectService.getById(id);
        m.addAttribute("project",         p);
        m.addAttribute("jobCards",        projectService.getJobCards(id));
        m.addAttribute("projectLabours",  projectService.getProjectLabours(id));
        m.addAttribute("advances",        advanceService.getAdvancesForProject(id));
        m.addAttribute("bills",           transactionRepo.findByProject_IdOrderByCreatedAtDesc(id));
        m.addAttribute("totalWork",       projectService.totalWorkValue(id));
        m.addAttribute("totalApproved",   projectService.totalApprovedExpenses(id));
        m.addAttribute("totalPending",    projectService.totalPendingExpenses(id));
        m.addAttribute("netProfit",       projectService.netProfitability(id));
        m.addAttribute("JobCardStatus",   JobCardStatus.values());
        m.addAttribute("ProjectStatus",   ProjectStatus.values());
        return "project/detail";
    }

    @GetMapping("/{id}/income-report")
    public String getIncomeReport(@PathVariable Long id, Model m) {
        Project p = projectService.getById(id);
        java.util.Map<String, Object> data = collectIncomeData(id);
        
        m.addAttribute("project",       p);
        m.addAttribute("entries",       data.get("entries"));
        m.addAttribute("totalReceived", data.get("totalReceived"));
        m.addAttribute("totalPending",  data.get("totalPending"));
        return "project/income-report";
    }

    @GetMapping("/{id}/income-report/pdf")
    public ResponseEntity<byte[]> getIncomeReportPdf(@PathVariable Long id) {
        Project p = projectService.getById(id);
        java.util.Map<String, Object> data = collectIncomeData(id);
        
        byte[] pdf = pdfService.generateProjectIncomeReport(
            p, 
            (java.util.List<java.util.Map<String, Object>>) data.get("entries"), 
            (java.math.BigDecimal) data.get("totalReceived"), 
            (java.math.BigDecimal) data.get("totalPending")
        );
        
        String fn = "Income_Report_" + p.getName().replaceAll("\\s+", "_") + ".pdf";
        return ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
            .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    private java.util.Map<String, Object> collectIncomeData(Long id) {
        List<AdvancePayment> advances = advanceService.getAdvancesForProject(id);
        List<Transaction> bills = transactionRepo.findByProject_IdOrderByCreatedAtDesc(id);
        
        java.util.List<java.util.Map<String, Object>> entries = new java.util.ArrayList<>();
        java.math.BigDecimal totalReceived = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalPending = java.math.BigDecimal.ZERO;

        // Bills
        for (Transaction b : bills) {
            java.math.BigDecimal cash = paymentService.totalReceivedForTransaction(b.getId());
            java.math.BigDecimal adv  = b.getAdvanceSettledAmount() != null ? b.getAdvanceSettledAmount() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal received = cash.add(adv);
            java.math.BigDecimal pending = b.getGrandTotal().subtract(received);
            if (pending.compareTo(java.math.BigDecimal.ZERO) < 0) pending = java.math.BigDecimal.ZERO;

            java.lang.String desc = "Invoice: " + b.getInvoiceNumber();
            if (adv.compareTo(java.math.BigDecimal.ZERO) > 0 && b.getAdvancePayment() != null) {
                desc += " | Adv Settled: " + b.getAdvancePayment().getAdvanceNumber() + " (₹" + adv + ")";
            }

            entries.add(new java.util.HashMap<>(java.util.Map.of(
                "date",     b.getCreatedAt(),
                "title",    desc,
                "total",    b.getGrandTotal(),
                "received", received,
                "pending",  pending
            )));
            totalReceived = totalReceived.add(received);
            totalPending = totalPending.add(pending);
        }

        // Unsettled Advances
        for (AdvancePayment a : advances) {
            if (a.getStatus() == AdvancePaymentStatus.SETTLED) continue;
            
            entries.add(new java.util.HashMap<>(java.util.Map.of(
                "date",     a.getCreatedAt(),
                "title",    "Advance: " + a.getAdvanceNumber(),
                "total",    a.getAmount(),
                "received", a.getAmount(),
                "pending",  java.math.BigDecimal.ZERO
            )));
            totalReceived = totalReceived.add(a.getAmount());
        }

        // Sort by date desc
        entries.sort((e1, e2) -> ((java.time.LocalDateTime)e2.get("date")).compareTo((java.time.LocalDateTime)e1.get("date")));

        return java.util.Map.of(
            "entries",       entries,
            "totalReceived", totalReceived,
            "totalPending",  totalPending
        );
    }

    // ── New / Edit form ───────────────────────────────────────────────────────
    @GetMapping("/new")
    public String newForm(Model m) {
        m.addAttribute("project",       new Project());
        m.addAttribute("ProjectStatus", ProjectStatus.values());
        return "project/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model m) {
        m.addAttribute("project",       projectService.getById(id));
        m.addAttribute("ProjectStatus", ProjectStatus.values());
        return "project/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Project project, RedirectAttributes ra) {
        Project saved = projectService.saveProject(project);
        ra.addFlashAttribute("success", "Project saved.");
        return "redirect:/admin/projects/" + saved.getId();
    }

    @DeleteMapping("/{id}") @ResponseBody
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.ok("Project deleted."));
    }

    // ── Job Card: new form ────────────────────────────────────────────────────
    @GetMapping("/{projectId}/jobcards/new")
    public String newJobCard(@PathVariable Long projectId, Model m) {
        JobCard jc = new JobCard();
        jc.setProject(projectService.getById(projectId));
        m.addAttribute("jobCard",       jc);
        m.addAttribute("project",       projectService.getById(projectId));
        m.addAttribute("engineers",     employeeRepo.findAll());
        m.addAttribute("JobCardStatus", JobCardStatus.values());
        return "jobcard/form";
    }

    @GetMapping("/jobcards/{id}/edit")
    public String editJobCard(@PathVariable Long id, Model m) {
        JobCard jc = projectService.getJobCard(id);
        m.addAttribute("jobCard",       jc);
        m.addAttribute("project",       jc.getProject());
        m.addAttribute("engineers",     employeeRepo.findAll());
        m.addAttribute("JobCardStatus", JobCardStatus.values());
        return "jobcard/form";
    }

    @PostMapping("/jobcards/save")
    public String saveJobCard(@ModelAttribute JobCard jobCard,
                              @RequestParam Long projectId,
                              @RequestParam(required = false) Long engineerId,
                              RedirectAttributes ra) {
        jobCard.setProject(projectService.getById(projectId));
        if (engineerId != null) {
            jobCard.setAssignedEngineer(
                employeeRepo.findById(engineerId).orElse(null));
        }
        projectService.saveJobCard(jobCard);
        ra.addFlashAttribute("success", "Job card saved.");
        return "redirect:/admin/projects/" + projectId;
    }

    @DeleteMapping("/jobcards/{id}") @ResponseBody
    public ResponseEntity<ApiResponse<?>> deleteJobCard(@PathVariable Long id) {
        projectService.deleteJobCard(id);
        return ResponseEntity.ok(ApiResponse.ok("Job card deleted."));
    }

    // ── Project Labours ───────────────────────────────────────────────────────
    @PostMapping("/{projectId}/labours/save")
    public String saveProjectLabour(@PathVariable Long projectId,
                                    @ModelAttribute ProjectLabour labour,
                                    RedirectAttributes ra) {
        labour.setProject(projectService.getById(projectId));
        projectService.saveProjectLabour(labour);
        ra.addFlashAttribute("success", "Project Labour added.");
        return "redirect:/admin/projects/" + projectId + "?tab=labours";
    }

    @DeleteMapping("/labours/{id}") @ResponseBody
    public ResponseEntity<ApiResponse<?>> deleteProjectLabour(@PathVariable Long id) {
        projectService.deleteProjectLabour(id);
        return ResponseEntity.ok(ApiResponse.ok("Project Labour deleted."));
    }

    @GetMapping("/{projectId}/labours/{labourId}")
    public String viewProjectLabour(@PathVariable Long projectId, @PathVariable Long labourId, Model m) {
        m.addAttribute("project", projectService.getById(projectId));
        m.addAttribute("labour", projectService.getProjectLabour(labourId));
        m.addAttribute("logs", projectService.getDailyLabourLogs(labourId));
        return "project/labour_detail";
    }

    @GetMapping("/{projectId}/labours/wages")
    public String projectLabourWages(@PathVariable Long projectId,
                                     @RequestParam(required = false) java.time.LocalDate from,
                                     @RequestParam(required = false) java.time.LocalDate to,
                                     @RequestParam(required = false) String labourName,
                                     Model m) {
        Project p = projectService.getById(projectId);
        List<DailyLabourLog> logs = projectService.getApprovedFilteredLabourWages(projectId, from, to, labourName);
        m.addAttribute("project", p);
        m.addAttribute("logs", logs);
        m.addAttribute("from", from);
        m.addAttribute("to", to);
        m.addAttribute("labourName", labourName);
        return "project/labour_wages";
    }

    @GetMapping("/{projectId}/labours/wages/export/pdf")
    public ResponseEntity<byte[]> exportLabourWagesPdf(@PathVariable Long projectId,
                                                       @RequestParam(required = false) java.time.LocalDate from,
                                                       @RequestParam(required = false) java.time.LocalDate to,
                                                       @RequestParam(required = false) String labourName) {
        Project p = projectService.getById(projectId);
        List<DailyLabourLog> logs = projectService.getApprovedFilteredLabourWages(projectId, from, to, labourName);
        byte[] pdf = labourPdfService.generate(logs, p, from, to);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        org.springframework.http.ContentDisposition contentDisposition = org.springframework.http.ContentDisposition.attachment()
                .filename("LabourWageReport_" + p.getName() + ".pdf", java.nio.charset.StandardCharsets.UTF_8)
                .build();
        headers.set(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
