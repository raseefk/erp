package com.supererp.erp.rbac.repository;

import com.supererp.erp.rbac.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
    List<Permission> findByFeatureIdOrderByActionAsc(String featureId);
    List<Permission> findByMenuIdOrderByActionAsc(String menuId);
}
