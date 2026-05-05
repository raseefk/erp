package com.levanto.flooring.repository;
import com.levanto.flooring.entity.Transaction;
import com.levanto.flooring.enums.PaymentStatus;
import com.levanto.flooring.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    @EntityGraph(attributePaths = {"customer", "items", "items.inventoryItem", "createdBy"})
    Optional<Transaction> findById(Long id);

    @EntityGraph(attributePaths = {"customer"})
    Page<Transaction> findAll(Pageable pageable);

    Optional<Transaction> findByInvoiceNumber(String invoiceNumber);

    @EntityGraph(attributePaths = {"customer"})
    Page<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus s, Pageable p);

    @EntityGraph(attributePaths = {"customer"})
    @Query("SELECT t FROM Transaction t JOIN t.customer c WHERE (:status IS NULL OR t.status=:status) AND (LOWER(t.invoiceNumber) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(t.quotationNumber) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(c.name) LIKE LOWER(CONCAT('%',:q,'%')) OR c.phone LIKE CONCAT('%',:q,'%'))")
    Page<Transaction> search(@Param("q") String q, @Param("status") TransactionStatus status, Pageable p);
    long countByStatus(TransactionStatus s);
    long countByPaymentStatus(PaymentStatus s);
    @Query("SELECT COALESCE(SUM(t.grandTotal),0) FROM Transaction t WHERE t.status=:s AND t.createdAt BETWEEN :from AND :to")
    BigDecimal sumGrandTotalByStatusAndDateRange(@Param("s") TransactionStatus s, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT MAX(t.quotationNumber) FROM Transaction t WHERE t.quotationNumber LIKE CONCAT(:prefix, '%')")
    String findMaxQuotationNumber(@Param("prefix") String prefix);

    @Query("SELECT MAX(t.invoiceNumber) FROM Transaction t WHERE t.invoiceNumber LIKE CONCAT(:prefix, '%')")
    String findMaxInvoiceNumber(@Param("prefix") String prefix);
}
