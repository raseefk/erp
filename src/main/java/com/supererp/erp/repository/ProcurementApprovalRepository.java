package com.supererp.erp.repository;

import com.supererp.erp.entity.ProcurementApproval;
import com.supererp.erp.enums.ProcurementApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcurementApprovalRepository extends JpaRepository<ProcurementApproval, Long> {

    List<ProcurementApproval> findByPurchaseOrderIdOrderByApprovalLevel(Long poId);

    Optional<ProcurementApproval> findByPurchaseOrderIdAndApprovalLevel(Long poId, int level);

    long countByStatus(ProcurementApprovalStatus status);

    @Query("SELECT pa FROM ProcurementApproval pa WHERE pa.status = 'PENDING' ORDER BY pa.createdAt DESC")
    List<ProcurementApproval> findAllPending();

    @Query("SELECT pa FROM ProcurementApproval pa WHERE pa.status = 'PENDING' " +
           "AND pa.approverRole = :role ORDER BY pa.createdAt")
    List<ProcurementApproval> findPendingByApproverRole(@Param("role") String role);
}
