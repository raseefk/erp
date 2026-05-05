package com.levanto.flooring.controller;

import com.levanto.flooring.service.ApprovalService;
import com.levanto.flooring.service.ExpenseService;
import com.levanto.flooring.service.FileStorageService;
import com.levanto.flooring.service.ProfitLossService;
import com.levanto.flooring.repository.ProjectExpenseRepository;
import com.levanto.flooring.service.SalaryService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final ProfitLossService  plService;
    private final ApprovalService    approvalService;
    private final ExpenseService     expenseService;
    private final SalaryService      salaryService;
    private final FileStorageService fileStorage;
    private final ProjectExpenseRepository projectExpRepo;

    // ── P&L Dashboard ─────────────────────────────────────────────────────────
    @GetMapping
    public String dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model m) {
        LocalDate f = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate t = to   != null ? to   : LocalDate.now();

        m.addAttribute("summary",       plService.calculate(f, t));
        m.addAttribute("monthly",       plService.last12Months());
        m.addAttribute("from", f);
        m.addAttribute("to",   t);
        return "finance/dashboard";
    }

    // ── Download expense attachment ───────────────────────────────────────────
    @GetMapping("/expenses/{id}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
        var expense = expenseService.getById(id);
        if (expense.getAttachmentPath() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path   filePath = fileStorage.resolve(expense.getAttachmentPath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();

            String mime = expense.getAttachmentMimeType() != null
                ? expense.getAttachmentMimeType() : "application/octet-stream";

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + expense.getAttachmentName() + "\"")
                .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Download project expense attachment ───────────────────────────────────
    @GetMapping("/project-expenses/{id}/attachment")
    public ResponseEntity<Resource> downloadProjectAttachment(@PathVariable Long id) {
        var pe = projectExpRepo.findById(id).orElse(null);
        if (pe == null || pe.getAttachmentPath() == null) return ResponseEntity.notFound().build();
        try {
            Path filePath = fileStorage.resolve(pe.getAttachmentPath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            String mime = pe.getAttachmentMimeType() != null ? pe.getAttachmentMimeType() : "application/octet-stream";
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pe.getAttachmentName() + "\"")
                .body(resource);
        } catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }
}