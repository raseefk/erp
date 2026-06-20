package com.supererp.erp.entity;

import com.supererp.erp.enums.ProcurementApprovalStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Procurement Approval record — supports amount-based escalation matrix.
 * Each PO can have multiple approval records (L1 → L2 → L3 based on amount).
 */
@Entity
@Table(name = "procurement_approvals")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ProcurementApproval extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "approval_level", nullable = false)
    private Integer approvalLevel; // 1 = L1, 2 = L2, 3 = L3

    @Column(name = "approver_name", length = 200)
    private String approverName;

    @Column(name = "approver_role", length = 100)
    private String approverRole;

    @Column(name = "min_amount", precision = 14, scale = 2)
    private BigDecimal minAmount; // threshold for this level

    @Column(name = "max_amount", precision = 14, scale = 2)
    private BigDecimal maxAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProcurementApprovalStatus status = ProcurementApprovalStatus.PENDING;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
