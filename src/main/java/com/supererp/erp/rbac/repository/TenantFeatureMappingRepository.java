package com.supererp.erp.rbac.repository;

import com.supererp.erp.rbac.entity.TenantFeatureMapping;
import com.supererp.erp.rbac.entity.TenantFeatureId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TenantFeatureMappingRepository
        extends JpaRepository<TenantFeatureMapping, TenantFeatureId> {

    List<TenantFeatureMapping> findByTenantId(UUID tenantId);
    boolean existsByTenantIdAndFeatureIdAndEnabledTrue(UUID tenantId, String featureId);
}
