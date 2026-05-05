package com.supererp.erp.rbac.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "tenant_feature_mapping")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(TenantFeatureId.class)
public class TenantFeatureMapping {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Id
    @Column(name = "feature_id", length = 60)
    private String featureId;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;
}
