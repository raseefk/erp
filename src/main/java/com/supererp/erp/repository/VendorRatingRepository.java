package com.supererp.erp.repository;

import com.supererp.erp.entity.VendorRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorRatingRepository extends JpaRepository<VendorRating, Long> {

    List<VendorRating> findByVendorIdOrderByRatingDateDesc(Long vendorId);

    @Query("SELECT AVG(vr.overallScore) FROM VendorRating vr WHERE vr.vendor.id = :vendorId")
    java.math.BigDecimal findAverageScoreByVendorId(@Param("vendorId") Long vendorId);

    @Query("SELECT vr FROM VendorRating vr LEFT JOIN vr.vendor v WHERE " +
           "(:q IS NULL OR :q = '' OR LOWER(v.name) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "ORDER BY vr.ratingDate DESC")
    Page<VendorRating> searchRatings(@Param("q") String q, Pageable pageable);

    @Query("SELECT vr.vendor.id as vendorId, vr.vendor.name as vendorName, " +
           "AVG(vr.overallScore) as avgScore, COUNT(vr) as totalRatings " +
           "FROM VendorRating vr GROUP BY vr.vendor.id, vr.vendor.name " +
           "ORDER BY AVG(vr.overallScore) DESC")
    List<Object[]> findVendorScorecardSummary();
}
