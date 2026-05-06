package com.supererp.erp.rbac.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "tenant_menu_mapping")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(TenantMenuId.class)
public class TenantMenuMapping {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Id
    @Column(name = "menu_id", length = 60)
    private String menuId;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;
}
