package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Audit log entity — single-tenant mode.
 * tenant_id column kept for schema compatibility but always set to APP_TENANT_ID.
 */
@Entity
@Table(name = "audit_log")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "old_state_hash", length = 64)
    private String oldStateHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_state_json")
    private Map<String, Object> newStateJson;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    void onCreate() {
        if (tenantId == null) {
            tenantId = com.supererp.erp.config.AppTenantConfig.APP_TENANT_ID;
        }
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
