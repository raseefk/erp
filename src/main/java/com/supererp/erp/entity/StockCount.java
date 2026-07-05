package com.supererp.erp.entity;

import com.supererp.erp.enums.StockCountStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Physical stock count / cycle counting session.
 * Compares system balance vs physical count and generates adjustments.
 */
@Entity
@Table(name = "stock_counts")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class StockCount extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "count_number", unique = true, nullable = false, length = 50)
    private String countNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private WarehouseLocation location; // null = full warehouse count

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StockCountStatus status = StockCountStatus.DRAFT;

    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "adjustment_posted")
    @Builder.Default
    private Boolean adjustmentPosted = false;

    @Column(length = 500)
    private String remarks;

    @Column(name = "conducted_by", length = 200)
    private String conductedBy;

    @Column(name = "approved_by", length = 200)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "stockCount", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockCountItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
