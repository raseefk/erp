package com.levanto.flooring.service;

import com.levanto.flooring.entity.*;
import com.levanto.flooring.enums.ExpenseCategory;
import com.levanto.flooring.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryService {

    private final EmployeeSalaryRepository salaryRepo;
    private final EmployeeRepository       employeeRepo;
    private final ExpenseRepository        expenseRepo;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    // ── Pay salary for one employee ───────────────────────────────────────────
    @Transactional
    public EmployeeSalary paySalary(Long employeeId, BigDecimal amount, LocalDate creditDate) {

        Employee emp       = employeeRepo.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        String monthYear   = creditDate.format(MONTH_FMT);

        // Prevent duplicate payment for same month
        if (salaryRepo.findByEmployeeAndSalaryMonthYear(emp, monthYear).isPresent()) {
            throw new IllegalStateException(
                "Salary for " + emp.getName() + " — " + monthYear + " has already been recorded.");
        }

        // 1. Create Expense entry first
        Expense expense = Expense.builder()
            .category(ExpenseCategory.SALARY)
            .description("Salary — " + emp.getName() + " (" + monthYear + ")")
            .amount(amount)
            .expenseDate(creditDate)
            .reference("EMP-" + employeeId)
            .employee(emp)
            .build();
        expense = expenseRepo.save(expense);

        // 2. Create EmployeeSalary record linked to that expense
        EmployeeSalary salary = EmployeeSalary.builder()
            .employee(emp)
            .salaryMonthYear(monthYear)
            .amount(amount)
            .salaryCreditedDate(creditDate)
            .linkedExpense(expense)
            .build();
        salary = salaryRepo.save(salary);

        log.info("Salary paid: {} — {} — ₹{}", emp.getName(), monthYear, amount);
        return salary;
    }

    // ── Read ──────────────────────────────────────────────────────────────────
    public List<EmployeeSalary> getByEmployee(Long employeeId) {
        Employee emp = employeeRepo.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        return salaryRepo.findByEmployeeOrderBySalaryCreditedDateDesc(emp);
    }

    public Page<EmployeeSalary> getAll(int page, int size, LocalDate from, LocalDate to) {
        Pageable pg = PageRequest.of(page, size, Sort.by("salaryCreditedDate").descending());
        if (from != null && to != null) return salaryRepo.findByDateRange(from, to, pg);
        return salaryRepo.findAllByOrderBySalaryCreditedDateDesc(pg);
    }

    public BigDecimal totalSalaryPaid(LocalDate from, LocalDate to) {
        return salaryRepo.sumSalaryByDateRange(from, to);
    }
}
