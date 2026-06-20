package com.supererp.erp.repository;

import com.supererp.erp.entity.BlanketPurchaseOrder;
import com.supererp.erp.enums.BlanketPoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlanketPurchaseOrderRepository extends JpaRepository<BlanketPurchaseOrder, Long> {

    @EntityGraph(attributePaths = {"vendor", "items", "items.inventoryItem"})
    Optional<BlanketPurchaseOrder> findById(Long id);

    @Query("SELECT b FROM BlanketPurchaseOrder b LEFT JOIN b.vendor v WHERE " +
           "(:q IS NULL OR :q = '' OR LOWER(b.bpoNumber) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(b.title) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(v.name) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "ORDER BY b.createdAt DESC")
    Page<BlanketPurchaseOrder> searchBpos(@Param("q") String q, Pageable pageable);

    @Query("SELECT b FROM BlanketPurchaseOrder b WHERE b.status = 'ACTIVE' AND b.endDate < :today")
    List<BlanketPurchaseOrder> findExpiredActiveBpos(@Param("today") LocalDate today);

    List<BlanketPurchaseOrder> findByVendorIdAndStatus(Long vendorId, BlanketPoStatus status);

    boolean existsByBpoNumber(String bpoNumber);
}
