package com.supererp.erp.repository;

import com.supererp.erp.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findByActiveTrueOrderByNameAsc();

    Optional<Warehouse> findByIsDefaultTrueAndTenantId(UUID tenantId);

    boolean existsByCodeAndTenantId(String code, UUID tenantId);

    @Query("SELECT w FROM Warehouse w WHERE w.active = true AND " +
           "(LOWER(w.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(w.code) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<Warehouse> search(@Param("q") String q);
}
