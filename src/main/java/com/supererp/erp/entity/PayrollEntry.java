package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Computed payslip line for one employee in one payroll run.
 * All statutory fields are stored even if zero — for audit purposes.
 */
@Entity
@Table(name = "payroll_entries",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "payroll_run_id", "employee_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PayrollEntry extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // ── Working days ──────────────────────────────────────────────────────────
    @Column(name = "total_working_days")
    private Integer totalWorkingDays;     // working days in the month (excl weekends+holidays)

    @Column(name = "days_present")
    private Integer daysPresent;

    @Column(name = "days_absent")
    private Integer daysAbsent;

    @Column(name = "days_leave")
    private Integer daysLeave;

    @Column(name = "days_lop")
    @Builder.Default
    private Integer daysLop = 0;          // Loss of Pay days

    // ── Earnings ──────────────────────────────────────────────────────────────
    @Column(name = "ctc_monthly", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal ctcMonthly = BigDecimal.ZERO;

    @Column(name = "gross_salary", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal grossSalary = BigDecimal.ZERO;   // after LOP deduction

    @Column(name = "basic", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal basic = BigDecimal.ZERO;

    @Column(name = "hra", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal hra = BigDecimal.ZERO;

    @Column(name = "da", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal da = BigDecimal.ZERO;

    @Column(name = "special_allowance", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal specialAllowance = BigDecimal.ZERO;

    @Column(name = "arrears", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal arrears = BigDecimal.ZERO;

    @Column(name = "leave_encashment", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal leaveEncashment = BigDecimal.ZERO;

    // ── Deductions ────────────────────────────────────────────────────────────
    @Column(name = "pf_employee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pfEmployee = BigDecimal.ZERO;    // 12% of basic (if enabled)

    @Column(name = "pf_employer", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pfEmployer = BigDecimal.ZERO;    // 12% of basic (informational)

    @Column(name = "esi_employee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal esiEmployee = BigDecimal.ZERO;   // 0.75% of gross (if enabled)

    @Column(name = "esi_employer", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal esiEmployer = BigDecimal.ZERO;   // 3.25% of gross (informational)

    @Column(name = "professional_tax", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal professionalTax = BigDecimal.ZERO;

    @Column(name = "tds", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tds = BigDecimal.ZERO;           // monthly TDS (annual/12)

    @Column(name = "total_deductions", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "net_salary", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal netSalary = BigDecimal.ZERO;

    // ── Bank / disbursement ───────────────────────────────────────────────────
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "disbursed")
    @Builder.Default
    private boolean disbursed = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
