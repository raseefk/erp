package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "subcontractor_running_bill_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SubcontractorRunningBillItem extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "running_bill_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private SubcontractorRunningBill runningBill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boq_item_id")
    private BoqItem boqItem;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal claimedQuantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal certifiedQuantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal claimedAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal certifiedAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @PrePersist
    @PreUpdate
    void recalculate() {
        BigDecimal safeClaimed = claimedQuantity != null ? claimedQuantity : BigDecimal.ZERO;
        BigDecimal safeCertified = certifiedQuantity != null ? certifiedQuantity : BigDecimal.ZERO;
        BigDecimal safeRate = rate != null ? rate : BigDecimal.ZERO;
        claimedAmount = safeClaimed.multiply(safeRate).setScale(2, RoundingMode.HALF_UP);
        certifiedAmount = safeCertified.multiply(safeRate).setScale(2, RoundingMode.HALF_UP);
    }
}
