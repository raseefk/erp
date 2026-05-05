package com.levanto.flooring.repository;
import com.levanto.flooring.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Page<com.levanto.flooring.projection.VendorSummary> findByActiveTrueOrderByNameAsc(Pageable p);

    @Query("SELECT v.id as id, v.name as name, v.email as email, v.phone as phone, " +
           "v.contactPerson as contactPerson, v.materialSupplied as materialSupplied, " +
           "v.gstNumber as gstNumber, v.bankName as bankName, v.bankAccountNumber as bankAccountNumber, " +
           "v.ifscCode as ifscCode, v.active as active FROM Vendor v " +
           "WHERE (:q IS NULL OR :q = '' OR LOWER(v.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(v.materialSupplied) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<com.levanto.flooring.projection.VendorSummary> searchSummaries(@Param("q") String q, Pageable p);


    List<Vendor> findAllByActiveTrueOrderByNameAsc();
    @Query("SELECT v FROM Vendor v WHERE LOWER(v.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(v.materialSupplied) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Vendor> search(@Param("q") String q, Pageable p);
}
