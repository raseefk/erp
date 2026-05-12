package com.supererp.erp.repository;

import com.supererp.erp.entity.SubcontractorRunningBill;
import com.supererp.erp.enums.SubcontractorBillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubcontractorRunningBillRepository extends JpaRepository<SubcontractorRunningBill, Long> {
    
    @EntityGraph(attributePaths = {"project", "vendor", "items", "items.boqItem", "items.boqItem.inventoryItem", "jobCard", "submittedBy", "certifiedBy"})
    Optional<SubcontractorRunningBill> findById(Long id);

    Optional<SubcontractorRunningBill> findByTenantIdAndBillNumber(UUID tenantId, String billNumber);
    
    @EntityGraph(attributePaths = {"project", "vendor"})
    List<SubcontractorRunningBill> findByProjectIdOrderByBillDateDesc(Long projectId);
    
    @EntityGraph(attributePaths = {"project", "vendor"})
    List<SubcontractorRunningBill> findByStatusOrderByBillDateDesc(SubcontractorBillStatus status);

    @EntityGraph(attributePaths = {"project", "vendor"})
    @Query("SELECT b FROM SubcontractorRunningBill b WHERE (:q IS NULL OR :q = '' OR LOWER(b.billNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(b.project.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(b.vendor.name) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY b.billDate DESC, b.id DESC")
    Page<SubcontractorRunningBill> search(@Param("q") String q, Pageable pageable);

    long countByProjectId(Long projectId);

    long countByJobCardId(Long jobCardId);
}
