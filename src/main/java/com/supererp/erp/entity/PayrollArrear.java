package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Arrear record — salary difference owed to an employee from a past period.
 * When a salary revision is applied retroactively, arrears are computed
 * and picked up automatically in the next payroll run.
 */
@Entity
@Table(name = "payroll_arrears")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PayrollArrear extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** The month for which arrear is due e.g. "January 2026" */
    @Column(name = "arrear_period", nullable = false, length = 30)
    private String arrearPeriod;

    @Column(name = "old_salary", precision = 12, scale = 2)
    private BigDecimal oldSalary;

    @Column(name = "new_salary", precision = 12, scale = 2)
    private BigDecimal newSalary;

    @Column(name = "arrear_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal arrearAmount;

    @Column(length = 500)
    private String reason;

    /** Whether this arrear has been included in a payroll run */
    @Column(name = "paid", nullable = false)
    @Builder.Default
    private boolean paid = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_entry_id")
    private PayrollEntry payrollEntry; // set when paid

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
