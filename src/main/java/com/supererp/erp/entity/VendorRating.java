package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vendor Rating / Scorecard entry — records performance for a specific PO/GRN transaction.
 */
@Entity
@Table(name = "vendor_ratings")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class VendorRating extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @Column(name = "rating_date", nullable = false)
    private LocalDate ratingDate;

    /** Quality score 1-5 */
    @Column(name = "quality_score", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal qualityScore = BigDecimal.ZERO;

    /** Delivery score 1-5 */
    @Column(name = "delivery_score", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal deliveryScore = BigDecimal.ZERO;

    /** Price competitiveness score 1-5 */
    @Column(name = "price_score", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal priceScore = BigDecimal.ZERO;

    /** Service/communication score 1-5 */
    @Column(name = "service_score", precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal serviceScore = BigDecimal.ZERO;

    /** Calculated overall score = avg of above */
    @Column(name = "overall_score", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal overallScore = BigDecimal.ZERO;

    @Column(name = "on_time_delivery")
    @Builder.Default
    private Boolean onTimeDelivery = true;

    @Column(name = "days_late")
    @Builder.Default
    private Integer daysLate = 0;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "rated_by", length = 200)
    private String ratedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
