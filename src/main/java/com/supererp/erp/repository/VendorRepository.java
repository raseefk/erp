package com.supererp.erp.repository;

import com.supererp.erp.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    List<Vendor> findByActiveTrueOrderByNameAsc();

    List<Vendor> findAllByActiveTrueOrderByNameAsc();

    long countByTenantId(java.util.UUID tenantId);

    @Query("SELECT v FROM Vendor v WHERE v.active = true AND " +
           "(:q IS NULL OR :q = '' OR LOWER(v.name) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(v.phone) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "ORDER BY v.name")
    Page<Vendor> searchVendors(@Param("q") String q, Pageable pageable);

    @Query("SELECT v.id as id, v.name as name, v.contactPerson as contactPerson, " +
           "v.phone as phone, v.email as email, v.gstNumber as gstNumber " +
           "FROM Vendor v WHERE v.active = true AND " +
           "(:q IS NULL OR :q = '' OR LOWER(v.name) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<com.supererp.erp.projection.VendorSummary> searchSummaries(@Param("q") String q, Pageable pageable);
}
