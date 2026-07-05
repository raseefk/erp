package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

/**
 * Individual line item in a Stock Transfer Order.
 */
@Entity
@Table(name = "stock_transfer_items")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class StockTransferItem extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_transfer_id", nullable = false)
    private StockTransfer stockTransfer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "requested_qty", nullable = false, precision = 12, scale = 3)
    private BigDecimal requestedQty;

    @Column(name = "transferred_qty", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal transferredQty = BigDecimal.ZERO;

    @Column(name = "received_qty", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal receivedQty = BigDecimal.ZERO;

    @Column(length = 20)
    private String unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_lot_id")
    private BatchLot batchLot;

    @Column(length = 300)
    private String remarks;
}
