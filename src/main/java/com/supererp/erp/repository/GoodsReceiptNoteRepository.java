package com.supererp.erp.repository;

import com.supererp.erp.entity.GoodsReceiptNote;
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
public interface GoodsReceiptNoteRepository extends JpaRepository<GoodsReceiptNote, Long> {

    @EntityGraph(attributePaths = {"purchaseOrder", "purchaseOrder.vendor", "items", "items.inventoryItem", "items.poItem"})
    Optional<GoodsReceiptNote> findById(Long id);

    @Query("SELECT g FROM GoodsReceiptNote g WHERE g.purchaseOrder.id = :poId ORDER BY g.createdAt DESC")
    List<GoodsReceiptNote> findByPurchaseOrderId(@Param("poId") Long poId);

    @Query("SELECT g FROM GoodsReceiptNote g LEFT JOIN g.purchaseOrder po LEFT JOIN po.vendor v " +
           "WHERE (:q IS NULL OR :q = '' OR LOWER(g.grnNumber) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(po.poNumber) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(v.name) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "ORDER BY g.createdAt DESC")
    Page<GoodsReceiptNote> searchGrns(@Param("q") String q, Pageable pageable);

    boolean existsByGrnNumber(String grnNumber);
}
