package com.levanto.flooring.repository;
import com.levanto.flooring.entity.Enquiry;
import com.levanto.flooring.enums.EnquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {
    long countByStatus(EnquiryStatus s);
    @Query("SELECT e.id as id, e.name as name, e.email as email, e.phone as phone, e.service as service, e.status as status, e.submittedAt as submittedAt FROM Enquiry e")
    Page<com.levanto.flooring.projection.EnquirySummary> findAllSummaries(Pageable p);

    @Query("SELECT e.id as id, e.name as name, e.email as email, e.phone as phone, e.service as service, e.status as status, e.submittedAt as submittedAt FROM Enquiry e " +
           "WHERE (:status IS NULL OR e.status=:status) " +
           "AND (:q IS NULL OR :q = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%',:q,'%')) OR e.phone LIKE CONCAT('%',:q,'%') OR LOWER(e.email) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<com.levanto.flooring.projection.EnquirySummary> searchSummaries(@Param("q") String q, @Param("status") EnquiryStatus status, Pageable p);
}
