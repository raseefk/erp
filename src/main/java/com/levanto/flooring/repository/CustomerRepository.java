package com.levanto.flooring.repository;
import com.levanto.flooring.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhone(String phone);
    List<Customer> findTop10ByNameContainingIgnoreCaseOrPhoneContaining(String name, String phone);
    @Query("SELECT c.id as id, c.name as name, c.email as email, c.phone as phone, c.address as address, c.gstNumber as gstNumber FROM Customer c")
    Page<com.levanto.flooring.projection.CustomerSummary> findAllSummaries(Pageable p);

    @Query("SELECT c.id as id, c.name as name, c.email as email, c.phone as phone, c.address as address, c.gstNumber as gstNumber FROM Customer c " +
           "WHERE (:q IS NULL OR :q = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%',:q,'%')) OR c.phone LIKE CONCAT('%',:q,'%') OR LOWER(c.gstNumber) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<com.levanto.flooring.projection.CustomerSummary> searchSummaries(@Param("q") String q, Pageable p);
}
