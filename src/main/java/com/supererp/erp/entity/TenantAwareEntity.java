package com.supererp.erp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Base class for all entities that previously carried a tenant_id column.
 * In single-tenant mode the tenant_id column is still present in the schema for
 * backward compatibility, but is always populated with the application's fixed UUID
 * and is never used for data isolation.
 *
 * The Hibernate multi-tenant @FilterDef / @Filter annotations have been removed.
 */
@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class TenantAwareEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /**
     * Auto-populates tenant_id with the application's fixed singleton UUID on INSERT.
     * This ensures existing schema constraints (NOT NULL) are satisfied without
     * requiring any caller to supply the value.
     */
    @jakarta.persistence.PrePersist
    public void ensureTenantId() {
        if (this.tenantId == null) {
            this.tenantId = com.supererp.erp.config.AppTenantConfig.APP_TENANT_ID;
        }
    }
}
