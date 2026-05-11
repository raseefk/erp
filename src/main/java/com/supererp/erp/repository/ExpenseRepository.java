package com.supererp.erp.repository;

import com.supererp.erp.entity.Expense;
import com.supererp.erp.enums.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // ── Date range only ───────────────────────────────────────────────────────
    Page<Expense> findByExpenseDateBetweenOrderByExpenseDateDesc(LocalDate from, LocalDate to, Pageable p);

    // ── Date range + category ─────────────────────────────────────────────────
    Page<Expense> findByCategoryAndExpenseDateBetweenOrderByExpenseDateDesc(
        ExpenseCategory cat, LocalDate from, LocalDate to, Pageable p);

    // ── For PDF export: all records matching filter, no pagination ─────────────
    List<Expense> findByExpenseDateBetweenOrderByCategoryAscExpenseDateDesc(LocalDate from, LocalDate to);
    List<Expense> findByCategoryAndExpenseDateBetweenOrderByExpenseDateDesc(
        ExpenseCategory cat, LocalDate from, LocalDate to);

    // ── Totals ────────────────────────────────────────────────────────────────
    @Query("SELECT COALESCE(SUM(e.amount),0) FROM Expense e WHERE e.expenseDate BETWEEN :from AND :to")
    BigDecimal sumByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT YEAR(e.expenseDate), MONTH(e.expenseDate), COALESCE(SUM(e.amount),0) FROM Expense e WHERE e.expenseDate BETWEEN :from AND :to GROUP BY YEAR(e.expenseDate), MONTH(e.expenseDate)")
    List<Object[]> sumGroupedByMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(e.amount),0) FROM Expense e WHERE e.category=:cat AND e.expenseDate BETWEEN :from AND :to")
    BigDecimal sumByCategoryAndDateRange(@Param("cat") ExpenseCategory cat,
                                        @Param("from") LocalDate from, @Param("to") LocalDate to);
}
