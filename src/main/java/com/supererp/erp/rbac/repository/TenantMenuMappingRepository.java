package com.supererp.erp.rbac.repository;

import com.supererp.erp.rbac.entity.TenantMenuMapping;
import com.supererp.erp.rbac.entity.TenantMenuId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TenantMenuMappingRepository
        extends JpaRepository<TenantMenuMapping, TenantMenuId> {

    List<TenantMenuMapping> findByTenantId(UUID tenantId);
    boolean existsByTenantIdAndMenuIdAndEnabledFalse(UUID tenantId, String menuId);
}
