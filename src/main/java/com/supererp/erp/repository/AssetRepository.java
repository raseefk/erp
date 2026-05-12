package com.supererp.erp.repository;

import com.supererp.erp.entity.Asset;
import com.supererp.erp.enums.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findByTenantIdAndAssetCode(UUID tenantId, String assetCode);
    List<Asset> findByTenantIdAndStatusOrderByNameAsc(UUID tenantId, AssetStatus status);
    long countByStatus(AssetStatus status);

    @Query("SELECT a FROM Asset a WHERE (:q IS NULL OR :q = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.assetCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.category) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY a.id DESC")
    Page<Asset> search(@Param("q") String q, Pageable pageable);
}
