package com.supererp.erp.repository;

import com.supererp.erp.entity.StockTransfer;
import com.supererp.erp.enums.StockTransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    @Query("SELECT st FROM StockTransfer st JOIN FETCH st.fromLocation fl JOIN FETCH fl.warehouse JOIN FETCH st.toLocation tl JOIN FETCH tl.warehouse ORDER BY st.createdAt DESC")
    Page<StockTransfer> findAllWithDetails(Pageable pageable);

    @Query("SELECT st FROM StockTransfer st JOIN FETCH st.fromLocation fl JOIN FETCH fl.warehouse JOIN FETCH st.toLocation tl JOIN FETCH tl.warehouse WHERE st.id = :id")
    Optional<StockTransfer> findByIdWithDetails(@Param("id") Long id);

    List<StockTransfer> findByStatusOrderByCreatedAtDesc(StockTransferStatus status);

    Optional<StockTransfer> findByTransferNumber(String transferNumber);

    @Query("SELECT st FROM StockTransfer st JOIN FETCH st.fromLocation fl JOIN FETCH fl.warehouse JOIN FETCH st.toLocation tl JOIN FETCH tl.warehouse " +
           "WHERE (:q IS NULL OR :q = '' OR LOWER(st.transferNumber) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<StockTransfer> searchWithDetails(@Param("q") String q, Pageable pageable);
}
