package com.supererp.erp.repository;

import com.supererp.erp.entity.StockCount;
import com.supererp.erp.enums.StockCountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockCountRepository extends JpaRepository<StockCount, Long> {

    @Query("SELECT sc FROM StockCount sc JOIN FETCH sc.warehouse ORDER BY sc.createdAt DESC")
    Page<StockCount> findAllWithWarehouse(Pageable pageable);

    @Query("SELECT sc FROM StockCount sc JOIN FETCH sc.warehouse LEFT JOIN FETCH sc.location LEFT JOIN FETCH sc.items i LEFT JOIN FETCH i.inventoryItem WHERE sc.id = :id")
    Optional<StockCount> findByIdWithItems(@Param("id") Long id);

    List<StockCount> findByStatusOrderByCreatedAtDesc(StockCountStatus status);

    Optional<StockCount> findByCountNumber(String countNumber);
}
