package com.levanto.flooring.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * employee_salaries — payroll disbursement records.
 * Each row represents one pay cycle for one employee.
 * Auto-linked to an Expense row (category = SALARY) on creation.
 */
@Entity
@Table(name = "employee_salaries",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"employee_id", "salary_month_year"},
           name = "uk_emp_salary_month"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeSalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Format: "April 2026" */
    @Column(nullable = false, length = 20)
    private String salaryMonthYear;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate salaryCreditedDate;

    /** Back-reference to the auto-created Expense row */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense linkedExpense;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
