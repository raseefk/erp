package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Per-employee payroll configuration.
 * Stores the CTC breakup percentages and statutory flags.
 * All deduction flags default to false — opt-in per requirement.
 */
@Entity
@Table(name = "payroll_configs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "employee_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PayrollConfig extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // ── CTC Breakup (% of gross salary) ──────────────────────────────────────
    /** Basic as % of gross. Default 40% */
    @Column(name = "basic_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal basicPct = new BigDecimal("40.00");

    /** HRA as % of gross. Default 20% */
    @Column(name = "hra_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal hraPct = new BigDecimal("20.00");

    /** DA as % of gross. Default 10% */
    @Column(name = "da_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal daPct = new BigDecimal("10.00");

    /** Special Allowance = remaining after Basic+HRA+DA (auto-calculated) */

    // ── Optional statutory deductions ─────────────────────────────────────────
    /** Enable PF deduction (employee 12% of basic, employer 12% of basic) */
    @Column(name = "pf_enabled", nullable = false)
    @Builder.Default
    private boolean pfEnabled = false;

    /** Override PF % — default 12 */
    @Column(name = "pf_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal pfPct = new BigDecimal("12.00");

    /** Enable ESI deduction (employee 0.75%, employer 3.25% of gross) */
    @Column(name = "esi_enabled", nullable = false)
    @Builder.Default
    private boolean esiEnabled = false;

    /** Enable Professional Tax deduction (slab-wise per state) */
    @Column(name = "pt_enabled", nullable = false)
    @Builder.Default
    private boolean ptEnabled = false;

    /** PT state code for slab lookup (e.g., "KA", "MH", "TN") */
    @Column(name = "pt_state", length = 5)
    private String ptState;

    /** Enable TDS on salary (per 26QB) */
    @Column(name = "tds_enabled", nullable = false)
    @Builder.Default
    private boolean tdsEnabled = false;

    /** Annual TDS amount (will be deducted equally per month = tdsAnnual/12) */
    @Column(name = "tds_annual", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal tdsAnnual = BigDecimal.ZERO;

    // ── Leave encashment ───────────────────────────────────────────────────────
    /** Number of casual leave days encashable at year-end */
    @Column(name = "encashable_leave_days")
    @Builder.Default
    private Integer encashableLeaveDays = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
