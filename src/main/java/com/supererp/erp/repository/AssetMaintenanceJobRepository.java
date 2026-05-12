package com.supererp.erp.repository;

import com.supererp.erp.entity.AssetMaintenanceJob;
import com.supererp.erp.enums.MaintenanceJobStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AssetMaintenanceJobRepository extends JpaRepository<AssetMaintenanceJob, Long> {
    List<AssetMaintenanceJob> findByScheduledDateBetweenOrderByScheduledDateAsc(LocalDate from, LocalDate to);
    List<AssetMaintenanceJob> findByStatusOrderByScheduledDateAsc(MaintenanceJobStatus status);

    @EntityGraph(attributePaths = {"assignedEmployee"})
    List<AssetMaintenanceJob> findByAssetIdOrderByScheduledDateDesc(Long assetId);
}
