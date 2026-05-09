package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * income_transactions — immutable ledger entries.
 * Every payment event (full or partial) against a Transaction creates one record here.
 */
@Entity
@Table(name = "income_transactions")
@Data @NoArgsConstructor @AllArgsConstructor @lombok.experimental.SuperBuilder
public class IncomeTransaction extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Link to the parent invoice/bill ─────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = true)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advance_payment_id")
    private AdvancePayment advancePayment;


    /** Mirrors Transaction.invoiceNumber for quick lookup / reporting */
    @Column(nullable = false, length = 50)
    private String inventoryNumber;

    /** Human-readable title, e.g. "Full Payment", "Advance", "2nd Instalment" */
    @Column(nullable = false, length = 200)
    private String title;

    /** Amount received in this specific payment event */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /**
     * Auto-generated description:
     * "Bill Amount: ₹X, Total Received: ₹Y, Pending Amount: ₹Z"
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate date;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}

