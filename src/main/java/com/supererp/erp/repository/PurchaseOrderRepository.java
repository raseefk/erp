package com.supererp.erp.repository;

import com.supererp.erp.entity.PurchaseOrder;
import com.supererp.erp.enums.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @EntityGraph(attributePaths = {"vendor", "items", "items.inventoryItem"})
    Optional<PurchaseOrder> findById(Long id);

    @Query("SELECT p.id as id, p.poNumber as poNumber, p.vendor.name as vendorName, p.orderDate as orderDate, " +
           "p.expectedDeliveryDate as expectedDeliveryDate, p.totalAmount as totalAmount, p.status as status " +
           "FROM PurchaseOrder p")
    Page<com.supererp.erp.projection.PurchaseOrderSummary> findAllSummaries(Pageable p);

    @Query("SELECT p.id as id, p.poNumber as poNumber, p.vendor.name as vendorName, p.orderDate as orderDate, " +
           "p.expectedDeliveryDate as expectedDeliveryDate, p.totalAmount as totalAmount, p.status as status " +
           "FROM PurchaseOrder p WHERE (:q IS NULL OR :q = '' OR " +
           "LOWER(p.poNumber) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.vendor.name) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<com.supererp.erp.projection.PurchaseOrderSummary> searchSummaries(@Param("q") String q, Pageable p);

    List<PurchaseOrder> findByStatusInOrderByOrderDateDesc(List<PurchaseOrderStatus> statuses);
}
