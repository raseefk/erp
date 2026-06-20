package com.supererp.erp.entity;

import com.supererp.erp.enums.BlanketPoStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Blanket Purchase Order / Rate Contract — pre-approved agreement with a vendor
 * for a period with committed rates, against which release POs are raised.
 */
@Entity
@Table(name = "blanket_purchase_orders")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class BlanketPurchaseOrder extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bpo_number", unique = true, nullable = false, length = 50)
    private String bpoNumber;

    @Column(nullable = false, length = 300)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BlanketPoStatus status = BlanketPoStatus.ACTIVE;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Total contracted value */
    @Column(name = "total_value", precision = 14, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO;

    /** Amount already consumed via release POs */
    @Column(name = "consumed_value", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal consumedValue = BigDecimal.ZERO;

    @Column(name = "payment_terms", length = 300)
    private String paymentTerms;

    @Column(columnDefinition = "TEXT")
    private String terms;

    @OneToMany(mappedBy = "blanketPo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BlanketPoItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
