package com.supererp.erp.repository;

import com.supererp.erp.entity.PreventiveMaintenancePlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PreventiveMaintenancePlanRepository extends JpaRepository<PreventiveMaintenancePlan, Long> {
    List<PreventiveMaintenancePlan> findByActiveTrueAndNextDueDateLessThanEqualOrderByNextDueDateAsc(LocalDate dueDate);

    @EntityGraph(attributePaths = {"assignedEmployee"})
    List<PreventiveMaintenancePlan> findByAssetIdOrderByNextDueDateAsc(Long assetId);
}
