package com.levanto.flooring.repository;

import com.levanto.flooring.entity.IncomeTransaction;
import com.levanto.flooring.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;

public interface IncomeTransactionRepository extends JpaRepository<IncomeTransaction, Long> {

    @EntityGraph(attributePaths = {"transaction", "transaction.customer"})
    List<IncomeTransaction> findByTransactionOrderByDateDesc(Transaction transaction);

    /** Sum of all income received for one invoice */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM IncomeTransaction i WHERE i.transaction.id = :txId")
    BigDecimal sumByTransactionId(@Param("txId") Long txId);

    /** Monthly income total for P&L */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM IncomeTransaction i WHERE i.date BETWEEN :from AND :to")
    BigDecimal sumByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Paged ledger view */
    @EntityGraph(attributePaths = {"transaction", "transaction.customer"})
    Page<IncomeTransaction> findAllByOrderByDateDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"transaction", "transaction.customer"})
    @Query("SELECT i FROM IncomeTransaction i WHERE i.date BETWEEN :from AND :to ORDER BY i.date DESC")
    Page<IncomeTransaction> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);
}
