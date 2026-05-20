package com.supererp.erp.repository;

import com.supererp.erp.entity.ErpEnquiry;
import com.supererp.erp.enums.EnquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ErpEnquiryRepository extends JpaRepository<ErpEnquiry, Long> {

    long countByStatus(EnquiryStatus s);

    @Query("SELECT e FROM ErpEnquiry e " +
           "WHERE (:status IS NULL OR e.status = :status) " +
           "AND (:q IS NULL OR :q = '' " +
           "OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(e.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR e.phone LIKE CONCAT('%', :q, '%') " +
           "OR LOWER(e.company) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<ErpEnquiry> search(@Param("q") String q, @Param("status") EnquiryStatus status, Pageable p);
}
