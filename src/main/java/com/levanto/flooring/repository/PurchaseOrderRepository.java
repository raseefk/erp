package com.levanto.flooring.repository;

import com.levanto.flooring.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"vendor", "items", "items.inventoryItem"})
    java.util.Optional<PurchaseOrder> findById(Long id);

    @Query("SELECT p.id as id, p.poNumber as poNumber, p.vendor.name as vendorName, p.orderDate as orderDate, p.expectedDeliveryDate as expectedDeliveryDate, p.totalAmount as totalAmount, p.status as status FROM PurchaseOrder p")
    Page<com.levanto.flooring.projection.PurchaseOrderSummary> findAllSummaries(Pageable p);

    @Query("SELECT p.id as id, p.poNumber as poNumber, p.vendor.name as vendorName, p.orderDate as orderDate, p.expectedDeliveryDate as expectedDeliveryDate, p.totalAmount as totalAmount, p.status as status FROM PurchaseOrder p " +
           "WHERE (:q IS NULL OR :q = '' OR LOWER(p.poNumber) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.vendor.name) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<com.levanto.flooring.projection.PurchaseOrderSummary> searchSummaries(@Param("q") String q, Pageable p);
}
