package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

/**
 * Individual line in a StockCount — system qty vs physically counted qty.
 */
@Entity
@Table(name = "stock_count_items")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class StockCountItem extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCount stockCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocation location;

    @Column(name = "system_qty", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal systemQty = BigDecimal.ZERO;

    @Column(name = "counted_qty", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal countedQty = BigDecimal.ZERO;

    @Column(name = "variance_qty", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal varianceQty = BigDecimal.ZERO;

    @Column(length = 20)
    private String unit;

    @Column(length = 300)
    private String remarks;

    @Column(name = "is_scanned")
    @Builder.Default
    private Boolean isScanned = false;
}
