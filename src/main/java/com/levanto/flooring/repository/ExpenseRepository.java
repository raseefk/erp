package com.levanto.flooring.repository;

import com.levanto.flooring.entity.Expense;
import com.levanto.flooring.enums.ExpenseCategory;
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

    @Query("SELECT COALESCE(SUM(e.amount),0) FROM Expense e WHERE e.category=:cat AND e.expenseDate BETWEEN :from AND :to")
    BigDecimal sumByCategoryAndDateRange(@Param("cat") ExpenseCategory cat,
                                        @Param("from") LocalDate from, @Param("to") LocalDate to);
}
