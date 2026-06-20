package com.supererp.erp.entity;

import com.supererp.erp.enums.GrnStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Goods Receipt Note — records physical receipt of goods against a PO.
 * Part of 3-way matching: PO → GRN → Vendor Invoice.
 */
@Entity
@Table(name = "goods_receipt_notes")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class GoodsReceiptNote extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grn_number", unique = true, nullable = false, length = 50)
    private String grnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GrnStatus status = GrnStatus.DRAFT;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "received_by", length = 200)
    private String receivedBy;

    @Column(name = "delivery_challan_no", length = 100)
    private String deliveryChallanNo;

    @Column(name = "vehicle_number", length = 50)
    private String vehicleNumber;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "total_received_value", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalReceivedValue = BigDecimal.ZERO;

    @OneToMany(mappedBy = "grn", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GrnItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
