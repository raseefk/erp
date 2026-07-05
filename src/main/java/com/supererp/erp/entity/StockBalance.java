package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Real-time stock balance of an item at a specific warehouse location.
 * Updated on every stock movement. One record per (item x location).
 */
@Entity
@Table(name = "stock_balances",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id","inventory_item_id","location_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class StockBalance extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocation location;

    @Column(name = "quantity_on_hand", precision = 14, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "quantity_reserved", precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    @Column(name = "quantity_available", precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal quantityAvailable = BigDecimal.ZERO;

    /** Weighted average cost per unit */
    @Column(name = "avg_cost", precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal avgCost = BigDecimal.ZERO;

    @Column(name = "reorder_point", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal reorderPoint = BigDecimal.ZERO;

    @Column(name = "reorder_qty", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal reorderQty = BigDecimal.ZERO;

    @Column(name = "alert_sent")
    @Builder.Default
    private Boolean alertSent = false;

    @Column(name = "last_movement_at")
    private LocalDateTime lastMovementAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
