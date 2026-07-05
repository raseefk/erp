package com.supererp.erp.repository;

import com.supererp.erp.entity.BatchLot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchLotRepository extends JpaRepository<BatchLot, Long> {

    @Query("SELECT bl FROM BatchLot bl JOIN FETCH bl.inventoryItem WHERE bl.inventoryItem.id = :itemId AND bl.active = true ORDER BY bl.createdAt DESC")
    List<BatchLot> findByItemIdActive(@Param("itemId") Long itemId);

    Optional<BatchLot> findByBatchNumberAndInventoryItemId(String batchNumber, Long itemId);

    @Query("SELECT bl FROM BatchLot bl JOIN FETCH bl.inventoryItem WHERE bl.active = true AND bl.quantityAvailable > 0 ORDER BY bl.inventoryItem.name, bl.expiryDate ASC NULLS LAST")
    Page<BatchLot> findActiveWithStock(Pageable pageable);

    @Query("SELECT bl FROM BatchLot bl JOIN FETCH bl.inventoryItem WHERE bl.expiryDate BETWEEN :from AND :to AND bl.active = true AND bl.quantityAvailable > 0")
    List<BatchLot> findExpiringBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT bl FROM BatchLot bl JOIN FETCH bl.inventoryItem WHERE bl.active = true ORDER BY bl.createdAt DESC")
    Page<BatchLot> findAllWithItem(Pageable pageable);
}
