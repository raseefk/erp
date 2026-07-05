package com.supererp.erp.entity;

import com.supererp.erp.enums.StockMovementType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Immutable audit log of every stock movement.
 * Never updated — only inserted. Drives FIFO/LIFO/WAC calculations.
 */
@Entity
@Table(name = "stock_ledger")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class StockLedger extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocation location;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private StockMovementType movementType;

    @Column(name = "transaction_number", nullable = false, length = 50)
    private String transactionNumber;

    /** Positive = IN, Negative = OUT */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_cost", precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "total_cost", precision = 16, scale = 4)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;

    /** Running balance after this movement */
    @Column(name = "balance_qty", precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal balanceQty = BigDecimal.ZERO;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Column(name = "reference_type", length = 50)
    private String referenceType; // PO, GRN, TRANSFER, ADJUSTMENT, STOCKCOUNT

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_lot_id")
    private BatchLot batchLot;

    @Column(length = 500)
    private String remarks;

    @Column(name = "created_by", length = 200)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
