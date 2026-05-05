package com.levanto.flooring.controller;

import com.levanto.flooring.dto.ApiResponse;
import com.levanto.flooring.service.EmployeeService;
import com.levanto.flooring.service.SalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin/salaries")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService  salaryService;
    private final EmployeeService employeeService;

    // ── Salary ledger page ────────────────────────────────────────────────────
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model m) {
        m.addAttribute("salariesPage",  salaryService.getAll(page, size, from, to));
        m.addAttribute("employees",     employeeService.getActive());
        m.addAttribute("from", from);
        m.addAttribute("to",   to);
        m.addAttribute("currentPage",   page);
        if (from != null && to != null)
            m.addAttribute("totalSalary", salaryService.totalSalaryPaid(from, to));
        return "salary/list";
    }

    // ── Pay salary (REST) ─────────────────────────────────────────────────────
    @PostMapping("/pay") @ResponseBody
    public ResponseEntity<ApiResponse<?>> pay(
            @RequestParam Long employeeId,
            @RequestParam BigDecimal amount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate creditDate) {
        try {
            var salary = salaryService.paySalary(employeeId, amount, creditDate);
            return ResponseEntity.ok(ApiResponse.ok(
                "Salary of ₹" + amount + " paid for " + salary.getSalaryMonthYear(), salary.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── History for one employee (REST) ───────────────────────────────────────
    @GetMapping("/employee/{id}") @ResponseBody
    public ResponseEntity<?> employeeHistory(@PathVariable Long id) {
        return ResponseEntity.ok(salaryService.getByEmployee(id));
    }
}
