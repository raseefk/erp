package com.levanto.flooring.entity;

import com.levanto.flooring.enums.ExpenseCategory;
import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "expenses")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Expense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expenseDate;

    @Column(length = 200)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    // ── File attachment (PDF or image) ───────────────────
    @Column(length = 255)
    private String attachmentName;       // original filename shown in UI

    @Column(length = 500)
    private String attachmentPath;       // server path, relative to uploads dir

    @Column(length = 100)
    private String attachmentMimeType;   // application/pdf, image/jpeg, etc.

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
