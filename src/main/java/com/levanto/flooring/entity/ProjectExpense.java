package com.levanto.flooring.entity;

import com.levanto.flooring.enums.ProjectExpenseCategory;
import com.levanto.flooring.enums.ProjectExpenseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bridge table: site-level costs stay here until Admin approves.
 * On approval → a matching record is written to CompanyExpense (the main ledger).
 * On rejection or log-deletion → record is purged (orphanRemoval = true from DailyLog).
 */
@Entity
@Table(name = "project_expenses")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectExpense {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Parent log (nullable for stand-alone misc entries not tied to a log) ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_log_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private DailyLog dailyLog;

    // ── Project + Job Card (denormalised for easy querying) ───────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false)
    private JobCard jobCard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectExpenseCategory category;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate expenseDate;

    // ── File attachment (optional receipt) ────────────────────────────────────
    @Column(length = 255)
    private String attachmentName;

    @Column(length = 500)
    private String attachmentPath;

    @Column(length = 100)
    private String attachmentMimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectExpenseStatus status = ProjectExpenseStatus.NEW;

    /** Set when status → APPROVED; points to the company ledger record created */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_expense_id")
    private Expense companyExpense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id")
    private AppUser submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private AppUser approvedBy;

    private LocalDateTime approvedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
