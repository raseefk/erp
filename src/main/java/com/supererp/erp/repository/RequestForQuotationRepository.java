package com.supererp.erp.repository;

import com.supererp.erp.entity.RequestForQuotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RequestForQuotationRepository extends JpaRepository<RequestForQuotation, Long> {

    @EntityGraph(attributePaths = {"items", "items.inventoryItem", "vendorResponses", "vendorResponses.vendor", "vendorResponses.responseItems", "vendorResponses.responseItems.rfqItem"})
    Optional<RequestForQuotation> findById(Long id);

    @Query("SELECT r FROM RequestForQuotation r WHERE " +
           "(:q IS NULL OR :q = '' OR LOWER(r.rfqNumber) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(r.title) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "ORDER BY r.createdAt DESC")
    Page<RequestForQuotation> searchRfqs(@Param("q") String q, Pageable pageable);

    boolean existsByRfqNumber(String rfqNumber);
}
