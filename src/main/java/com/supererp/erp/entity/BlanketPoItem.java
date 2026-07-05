package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

/**
 * Rate contract line item within a Blanket PO.
 */
@Entity
@Table(name = "blanket_po_items")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class BlanketPoItem extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blanket_po_id", nullable = false)
    private BlanketPurchaseOrder blanketPo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(name = "agreed_unit_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal agreedUnitPrice = BigDecimal.ZERO;

    @Column(length = 20)
    private String unit;

    /** Max qty agreed in contract */
    @Column(name = "max_quantity", precision = 12, scale = 3)
    private BigDecimal maxQuantity;

    /** Qty already released */
    @Column(name = "released_quantity", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal releasedQuantity = BigDecimal.ZERO;
}
