package com.levanto.flooring.controller;

import com.levanto.flooring.dto.ApiResponse;
import com.levanto.flooring.entity.Expense;
import com.levanto.flooring.enums.ExpenseCategory;
import com.levanto.flooring.service.ExpensePdfService;
import com.levanto.flooring.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller @RequestMapping("/admin/expenses") @RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService    service;
    private final ExpensePdfService pdfService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category,
            Model m) {

        LocalDate f  = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate t  = to   != null ? to   : LocalDate.now();
        ExpenseCategory cat = parseCategory(category);

        m.addAttribute("expensesPage",  service.getAll(page, size, f, t, cat));
        m.addAttribute("from",          f);
        m.addAttribute("to",            t);
        m.addAttribute("categoryFilter", category);
        m.addAttribute("totalExpenses", cat != null
            ? service.totalForCategory(cat, f, t)
            : service.totalForPeriod(f, t));
        m.addAttribute("categories",    ExpenseCategory.values());
        m.addAttribute("currentPage",   page);
        return "expense/list";
    }

    /** Download PDF of currently filtered expenses */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category) {

        LocalDate f  = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate t  = to   != null ? to   : LocalDate.now();
        ExpenseCategory cat = parseCategory(category);

        List<Expense> expenses = service.getAllForExport(f, t, cat);
        byte[] pdf = pdfService.generate(expenses, f, t, cat);

        String fmt  = DateTimeFormatter.ofPattern("ddMMMyyyy").format(f)
                    + "_to_" + DateTimeFormatter.ofPattern("ddMMMyyyy").format(t);
        String name = "Expenses_" + (cat != null ? cat.name() + "_" : "") + fmt + ".pdf";

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
            .body(pdf);
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        m.addAttribute("expense",    new Expense());
        m.addAttribute("categories", ExpenseCategory.values());
        return "expense/form";
    }

    @PostMapping(value = "/save", consumes = "multipart/form-data")
    public String save(@ModelAttribute Expense expense,
                       @RequestParam(value = "file", required = false) MultipartFile file,
                       RedirectAttributes ra) {
        try {
            service.save(expense, file);
            ra.addFlashAttribute("success", "Expense recorded.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/expenses";
    }

    @DeleteMapping("/{id}") @ResponseBody
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted."));
    }

    private ExpenseCategory parseCategory(String s) {
        if (s == null || s.isBlank()) return null;
        try { return ExpenseCategory.valueOf(s.toUpperCase()); } catch (Exception e) { return null; }
    }
}
