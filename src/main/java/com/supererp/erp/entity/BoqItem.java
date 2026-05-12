package com.supererp.erp.entity;

import com.supererp.erp.enums.BoqItemStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boq_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class BoqItem extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boq_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private BillOfQuantity boq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal completedQuantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 7, scale = 2)
    @Builder.Default
    private BigDecimal completionPercent = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BoqItemStatus status = BoqItemStatus.NOT_STARTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_job_card_id")
    private JobCard linkedJobCard;

    @OneToMany(mappedBy = "boqItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<BoqProgressEntry> progressEntries = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        recalculate();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
        recalculate();
    }

    private void recalculate() {
        BigDecimal safeQuantity = quantity != null ? quantity : BigDecimal.ZERO;
        BigDecimal safeRate = rate != null ? rate : BigDecimal.ZERO;
        BigDecimal safeCompleted = completedQuantity != null ? completedQuantity : BigDecimal.ZERO;
        amount = safeQuantity.multiply(safeRate).setScale(2, RoundingMode.HALF_UP);
        completionPercent = safeQuantity.signum() > 0
            ? safeCompleted.multiply(BigDecimal.valueOf(100)).divide(safeQuantity, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    }
}
