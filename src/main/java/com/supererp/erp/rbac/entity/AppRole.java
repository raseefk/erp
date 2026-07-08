package com.supererp.erp.rbac.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(exclude = {"permissions"})
public class AppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Kept for schema compatibility — always set to AppTenantConfig.APP_TENANT_ID.
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Column(name = "is_system")
    @Builder.Default
    private boolean system = false;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        if (tenantId == null) {
            tenantId = com.supererp.erp.config.AppTenantConfig.APP_TENANT_ID;
        }
    }
}
