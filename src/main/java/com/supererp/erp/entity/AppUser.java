package com.supererp.erp.entity;

import com.supererp.erp.rbac.entity.AppRole;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Application user entity.
 * tenant_id column is kept for schema compatibility and always set to
 * AppTenantConfig.APP_TENANT_ID in single-tenant mode.
 * The unique constraint is on (tenant_id, username) — effectively just username.
 */
@Entity
@Table(name = "app_users",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "username"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Kept for schema compatibility — always set to AppTenantConfig.APP_TENANT_ID.
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(length = 255)
    private String email;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<AppRole> roles = new HashSet<>();

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (tenantId == null) {
            tenantId = com.supererp.erp.config.AppTenantConfig.APP_TENANT_ID;
        }
    }

    @PreUpdate
    void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
