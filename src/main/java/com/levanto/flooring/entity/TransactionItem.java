package com.levanto.flooring.entity;

import com.levanto.flooring.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "transaction_items")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType itemType = ItemType.PRODUCT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem; // null for custom/service items

    @Column(nullable = false, length = 300)
    private String description;

    @Column(length = 20)
    private String hsnSacCode;

    // ── Measurement fields ──────────────────────────────
    @Column(precision = 12, scale = 3)
    private BigDecimal squareFeet = BigDecimal.ZERO;  // for flooring

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity   = BigDecimal.ONE;   // generic qty

    @Column(length = 20)
    private String unit = "SQF";

    // ── Price snapshot at transaction time ──────────────
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerUnit = BigDecimal.ZERO; // rate per sqft or per unit

    // ── GST ─────────────────────────────────────────────
    @Column(precision = 5, scale = 2)
    private BigDecimal gstPercent = BigDecimal.ZERO;

    // ── Calculated totals (stored for PDF accuracy) ─────
    @Column(precision = 14, scale = 2) private BigDecimal baseAmount = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2) private BigDecimal cgstAmount = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2) private BigDecimal sgstAmount = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2) private BigDecimal igstAmount = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2) private BigDecimal totalAmount = BigDecimal.ZERO;
}
