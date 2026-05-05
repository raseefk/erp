package com.levanto.flooring.entity;

import com.levanto.flooring.enums.PaymentStatus;
import com.levanto.flooring.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "transactions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50)
    private String invoiceNumber; // generated only on FINAL_BILL conversion

    @Column(unique = true, length = 50)
    private String quotationNumber; // set at creation for quotations

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.QUOTATION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TransactionItem> items = new ArrayList<>();

    // ── GST Toggles ────────────────────────────────────
    /** If false → GST is 0 for ALL items */
    @Column(nullable = false)
    private boolean gstEnabled = true;

    /** If true → GST applied to Products AND Services. If false → GST only on Services */
    @Column(nullable = false)
    private boolean taxAllItems = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private com.levanto.flooring.enums.GstType gstType = com.levanto.flooring.enums.GstType.LOCAL;

    // ── Financials (stored for archival accuracy) ───────
    @Column(precision = 14, scale = 2) private BigDecimal subtotal    = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2) private BigDecimal totalCgst   = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2) private BigDecimal totalSgst   = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2) private BigDecimal totalIgst   = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2) private BigDecimal grandTotal  = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2) private BigDecimal amountPaid  = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ── Audit ────────────────────────────────────────────
    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime convertedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private AppUser createdBy;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
