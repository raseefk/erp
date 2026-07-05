package com.supererp.erp.repository;

import com.supererp.erp.entity.StockBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    Optional<StockBalance> findByInventoryItemIdAndLocationId(Long itemId, Long locationId);

    @Query("SELECT sb FROM StockBalance sb JOIN FETCH sb.inventoryItem JOIN FETCH sb.location l JOIN FETCH l.warehouse WHERE sb.location.id = :locationId ORDER BY sb.inventoryItem.name ASC")
    List<StockBalance> findByLocationIdWithDetails(@Param("locationId") Long locationId);

    @Query("SELECT sb FROM StockBalance sb JOIN FETCH sb.location l JOIN FETCH l.warehouse WHERE sb.inventoryItem.id = :itemId ORDER BY l.warehouse.name ASC, l.fullAddress ASC")
    List<StockBalance> findByInventoryItemIdWithDetails(@Param("itemId") Long itemId);

    @Query("SELECT sb FROM StockBalance sb JOIN FETCH sb.inventoryItem JOIN FETCH sb.location l JOIN FETCH l.warehouse WHERE l.warehouse.id = :warehouseId ORDER BY sb.inventoryItem.name ASC")
    List<StockBalance> findByWarehouseIdWithDetails(@Param("warehouseId") Long warehouseId);

    @Query("SELECT sb FROM StockBalance sb JOIN FETCH sb.inventoryItem JOIN FETCH sb.location WHERE sb.quantityAvailable <= sb.reorderPoint AND sb.reorderPoint > 0")
    List<StockBalance> findBelowReorderPoint();

    @Query("SELECT sb FROM StockBalance sb JOIN FETCH sb.inventoryItem JOIN FETCH sb.location WHERE sb.tenantId = :tenantId AND sb.quantityAvailable <= sb.reorderPoint AND sb.reorderPoint > 0")
    List<StockBalance> findBelowReorderPointByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT SUM(sb.quantityOnHand) FROM StockBalance sb WHERE sb.inventoryItem.id = :itemId")
    BigDecimal sumQuantityByItem(@Param("itemId") Long itemId);
}
