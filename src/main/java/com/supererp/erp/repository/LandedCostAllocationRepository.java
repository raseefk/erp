package com.supererp.erp.repository;

import com.supererp.erp.entity.LandedCostAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LandedCostAllocationRepository extends JpaRepository<LandedCostAllocation, Long> {

    List<LandedCostAllocation> findByPurchaseOrderIdOrderByCreatedAt(Long poId);

    @Query("SELECT SUM(l.amount) FROM LandedCostAllocation l WHERE l.purchaseOrder.id = :poId")
    BigDecimal sumByPurchaseOrderId(@Param("poId") Long poId);
}
