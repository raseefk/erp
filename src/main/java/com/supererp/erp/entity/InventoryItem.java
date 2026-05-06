package com.supererp.erp.entity;

import com.supererp.erp.enums.ItemType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "inventory_items")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class InventoryItem extends TenantAwareEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType itemType = ItemType.PRODUCT;

    @Column(precision = 12, scale = 2)
    private BigDecimal currentPrice = BigDecimal.ZERO;

    // Only relevant for PRODUCT type
    private Integer stockQuantity = 0;

    @Column(length = 20)
    private String hsnSacCode;

    @Column(length = 20)
    private String unit = "SQF"; // SQF, NOS, RFT, MTR, BAG

    @Column(nullable = false)
    private boolean active = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
