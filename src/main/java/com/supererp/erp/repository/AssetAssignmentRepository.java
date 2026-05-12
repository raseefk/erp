package com.supererp.erp.repository;

import com.supererp.erp.entity.AssetAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, Long> {
    
    @EntityGraph(attributePaths = {"project", "employee"})
    Optional<AssetAssignment> findFirstByAssetIdAndReturnedAtIsNullOrderByAssignedFromDesc(Long assetId);

    @EntityGraph(attributePaths = {"project", "employee"})
    List<AssetAssignment> findByAssetIdOrderByAssignedFromDesc(Long assetId);
}
