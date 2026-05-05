package com.levanto.flooring.repository;

import com.levanto.flooring.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
    
    @Query("SELECT poi FROM PurchaseOrderItem poi WHERE poi.inventoryItem.id = :itemId AND poi.purchaseOrder.status = 'RECEIVED' ORDER BY poi.purchaseOrder.orderDate DESC LIMIT 1")
    Optional<PurchaseOrderItem> findLatestReceivedPrice(@Param("itemId") Long itemId);
}
