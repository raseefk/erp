package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "daily_logs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyLog extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false)
    private JobCard jobCard;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate logDate;

    @Column(columnDefinition = "TEXT")
    private String progressDescription;

    // ── Work Value ────────────────────────────────────────────────────
    /** Estimated revenue / market value of today's completed work */
    @Column(precision = 14, scale = 2)
    private BigDecimal workValue = BigDecimal.ZERO;

    // ── Labour ────────────────────────────────────────────────────────
    @Column(nullable = false)
    private Integer numberOfLabours = 0;

    @Column(precision = 10, scale = 2)
    private BigDecimal dailyWageRate = BigDecimal.ZERO;

    /** Computed: numberOfLabours × dailyWageRate */
    @Column(precision = 14, scale = 2)
    private BigDecimal totalLabourCost = BigDecimal.ZERO;

    // ── Logged by ─────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_by_id")
    private AppUser loggedBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ── Related project expenses (cascade-deleted when log removed) ───
    @OneToMany(mappedBy = "dailyLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private java.util.Set<ProjectExpense> projectExpenses = new java.util.LinkedHashSet<>();

    @OneToMany(mappedBy = "dailyLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private java.util.Set<DailyLabourLog> labourLogs = new java.util.LinkedHashSet<>();

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}

