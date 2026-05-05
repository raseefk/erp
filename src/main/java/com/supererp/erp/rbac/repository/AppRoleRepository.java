package com.supererp.erp.rbac.repository;

import com.supererp.erp.rbac.entity.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppRoleRepository extends JpaRepository<AppRole, Long> {
    List<AppRole> findByTenantIdOrderByNameAsc(UUID tenantId);
    Optional<AppRole> findByTenantIdAndName(UUID tenantId, String name);
    boolean existsByTenantIdAndName(UUID tenantId, String name);

    @Query("SELECT r FROM AppRole r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<AppRole> findByIdWithPermissions(Long id);
}
