package com.levanto.flooring.repository;
import com.levanto.flooring.entity.ProjectExpense;
import com.levanto.flooring.enums.ProjectExpenseCategory;
import com.levanto.flooring.enums.ProjectExpenseStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

public interface ProjectExpenseRepository extends JpaRepository<ProjectExpense, Long> {

    @EntityGraph(attributePaths = {"project", "dailyLog", "jobCard", "submittedBy"})
    Optional<ProjectExpense> findById(Long id);

    @EntityGraph(attributePaths = {"project", "dailyLog", "jobCard", "submittedBy"})
    List<ProjectExpense> findByStatusOrderByCreatedAtDesc(ProjectExpenseStatus status);

    @EntityGraph(attributePaths = {"project", "dailyLog", "jobCard", "submittedBy"})
    Page<ProjectExpense> findByStatusOrderByCreatedAtDesc(ProjectExpenseStatus status, Pageable p);

    @EntityGraph(attributePaths = {"project", "dailyLog", "jobCard", "submittedBy"})
    List<ProjectExpense> findByProject_IdAndStatusOrderByExpenseDateDesc(Long projectId, ProjectExpenseStatus status);

    long countByStatus(ProjectExpenseStatus status);

    long countByJobCard_IdAndStatus(Long jobCardId, ProjectExpenseStatus status);

    @Query("SELECT COALESCE(SUM(e.amount),0) FROM ProjectExpense e WHERE e.project.id=:pid AND e.status=:s")
    BigDecimal sumByProjectAndStatus(@Param("pid") Long projectId, @Param("s") ProjectExpenseStatus status);

    @Query("SELECT COALESCE(SUM(e.amount),0) FROM ProjectExpense e WHERE e.project.id=:pid AND e.status='APPROVED' AND e.expenseDate=:d")
    BigDecimal sumApprovedByProjectAndDate(@Param("pid") Long pid, @Param("d") LocalDate date);

    @Query("SELECT COALESCE(SUM(e.amount),0) FROM ProjectExpense e WHERE e.project.id=:pid AND e.status='APPROVED' AND e.expenseDate BETWEEN :from AND :to")
    BigDecimal sumApprovedByProjectAndDateRange(@Param("pid") Long pid, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @EntityGraph(attributePaths = {"project", "dailyLog", "jobCard", "submittedBy"})
    @Query("SELECT e FROM ProjectExpense e WHERE e.project.id=:pid AND e.status='NEW' ORDER BY e.expenseDate DESC")
    List<ProjectExpense> findNewByProject(@Param("pid") Long pid);
}
