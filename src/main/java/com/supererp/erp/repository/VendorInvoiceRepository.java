package com.supererp.erp.repository;

import com.supererp.erp.entity.VendorInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorInvoiceRepository extends JpaRepository<VendorInvoice, Long> {

    @EntityGraph(attributePaths = {"purchaseOrder", "grn", "vendor"})
    Optional<VendorInvoice> findById(Long id);

    List<VendorInvoice> findByPurchaseOrderIdOrderByCreatedAtDesc(Long poId);

    @Query("SELECT vi FROM VendorInvoice vi LEFT JOIN vi.vendor v LEFT JOIN vi.purchaseOrder po " +
           "WHERE (:q IS NULL OR :q = '' OR LOWER(vi.invoiceNumber) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(v.name) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(po.poNumber) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "ORDER BY vi.createdAt DESC")
    Page<VendorInvoice> searchInvoices(@Param("q") String q, Pageable pageable);

    @Query("SELECT COUNT(vi) FROM VendorInvoice vi WHERE vi.matchStatus = 'DISCREPANCY'")
    long countDiscrepancies();
}
