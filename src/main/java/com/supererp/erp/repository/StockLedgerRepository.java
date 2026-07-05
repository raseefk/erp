package com.supererp.erp.repository;

import com.supererp.erp.entity.StockLedger;
import com.supererp.erp.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedger, Long> {

    @Query("SELECT sl FROM StockLedger sl JOIN FETCH sl.inventoryItem JOIN FETCH sl.location l JOIN FETCH l.warehouse WHERE sl.inventoryItem.id = :itemId ORDER BY sl.createdAt DESC")
    Page<StockLedger> findByItemIdWithDetails(@Param("itemId") Long itemId, Pageable pageable);

    @Query("SELECT sl FROM StockLedger sl JOIN FETCH sl.inventoryItem JOIN FETCH sl.location l JOIN FETCH l.warehouse WHERE sl.location.id = :locationId ORDER BY sl.createdAt DESC")
    Page<StockLedger> findByLocationIdWithDetails(@Param("locationId") Long locationId, Pageable pageable);

    @Query("SELECT sl FROM StockLedger sl JOIN FETCH sl.inventoryItem JOIN FETCH sl.location l JOIN FETCH l.warehouse WHERE l.warehouse.id = :warehouseId AND sl.movementDate BETWEEN :from AND :to ORDER BY sl.createdAt DESC")
    List<StockLedger> findByWarehouseAndDateRange(@Param("warehouseId") Long warehouseId,
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT sl FROM StockLedger sl JOIN FETCH sl.inventoryItem JOIN FETCH sl.location WHERE sl.referenceType = :refType AND sl.referenceId = :refId ORDER BY sl.createdAt ASC")
    List<StockLedger> findByReference(@Param("refType") String refType, @Param("refId") Long refId);

    @Query("SELECT sl FROM StockLedger sl JOIN FETCH sl.inventoryItem JOIN FETCH sl.location l JOIN FETCH l.warehouse ORDER BY sl.createdAt DESC")
    Page<StockLedger> findAllWithDetails(Pageable pageable);
}
