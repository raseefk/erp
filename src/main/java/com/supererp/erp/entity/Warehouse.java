package com.supererp.erp.entity;

import com.supererp.erp.enums.CostingMethod;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A physical or virtual warehouse / storage facility.
 * Each tenant can have multiple warehouses.
 */
@Entity
@Table(name = "warehouses")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Warehouse extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String code; // e.g. WH-001

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "manager_name", length = 200)
    private String managerName;

    @Column(name = "manager_phone", length = 30)
    private String managerPhone;

    @Column(name = "manager_email", length = 200)
    private String managerEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "costing_method", nullable = false)
    @Builder.Default
    private CostingMethod costingMethod = CostingMethod.WEIGHTED_AVERAGE;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WarehouseLocation> locations = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
