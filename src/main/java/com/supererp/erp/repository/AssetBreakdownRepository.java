package com.supererp.erp.repository;

import com.supererp.erp.entity.AssetBreakdown;
import com.supererp.erp.enums.BreakdownStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetBreakdownRepository extends JpaRepository<AssetBreakdown, Long> {

    @EntityGraph(attributePaths = {"assignedEmployee", "reportedBy"})
    List<AssetBreakdown> findByAssetIdOrderByReportedAtDesc(Long assetId);
    
    List<AssetBreakdown> findByStatusOrderByReportedAtDesc(BreakdownStatus status);
}
