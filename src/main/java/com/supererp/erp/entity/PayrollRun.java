package com.supererp.erp.entity;

import com.supererp.erp.enums.PayrollRunStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single monthly payroll run.
 * Contains one PayrollEntry per employee.
 */
@Entity
@Table(name = "payroll_runs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "pay_month", "pay_year"}))
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PayrollRun extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pay_month", nullable = false)
    private Integer payMonth; // 1–12

    @Column(name = "pay_year", nullable = false)
    private Integer payYear;

    /** "April 2026" */
    @Column(name = "pay_period_label", length = 30)
    private String payPeriodLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PayrollRunStatus status = PayrollRunStatus.DRAFT;

    @Column(name = "total_gross", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalGross = BigDecimal.ZERO;

    @Column(name = "total_deductions", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "total_net", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalNet = BigDecimal.ZERO;

    @Column(name = "approved_by", length = 200)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(length = 500)
    private String remarks;

    @OneToMany(mappedBy = "payrollRun", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PayrollEntry> entries = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
