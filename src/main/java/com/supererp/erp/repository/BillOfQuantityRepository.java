package com.supererp.erp.repository;

import com.supererp.erp.entity.BillOfQuantity;
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
public interface BillOfQuantityRepository extends JpaRepository<BillOfQuantity, Long> {
    
    @EntityGraph(attributePaths = {"project"})
    Optional<BillOfQuantity> findById(Long id);

    Optional<BillOfQuantity> findByTenantIdAndBoqNumber(UUID tenantId, String boqNumber);
    
    @EntityGraph(attributePaths = {"project"})
    List<BillOfQuantity> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    @EntityGraph(attributePaths = {"project"})
    @Query("SELECT b FROM BillOfQuantity b WHERE (:q IS NULL OR :q = '' OR LOWER(b.boqNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(b.project.name) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY b.id DESC")
    Page<BillOfQuantity> search(@Param("q") String q, Pageable pageable);
}
