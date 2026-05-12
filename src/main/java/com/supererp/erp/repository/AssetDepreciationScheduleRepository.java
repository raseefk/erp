package com.supererp.erp.repository;

import com.supererp.erp.entity.AssetDepreciationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetDepreciationScheduleRepository extends JpaRepository<AssetDepreciationSchedule, Long> {
    List<AssetDepreciationSchedule> findByAssetIdOrderByPeriodStartAsc(Long assetId);
    void deleteByAssetIdAndPostedFalse(Long assetId);
}
