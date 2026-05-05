package com.levanto.flooring.repository;
import com.levanto.flooring.entity.DailyLog;
import com.levanto.flooring.entity.JobCard;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {

    @EntityGraph(attributePaths = {"jobCard", "jobCard.project", "loggedBy", "projectExpenses", "labourLogs", "labourLogs.projectLabour"})
    Optional<DailyLog> findById(Long id);

    @EntityGraph(attributePaths = {"jobCard", "jobCard.project", "loggedBy", "projectExpenses", "labourLogs", "labourLogs.projectLabour"})
    List<DailyLog> findByJobCardOrderByLogDateDesc(JobCard jobCard);

    @EntityGraph(attributePaths = {"jobCard", "jobCard.project", "loggedBy", "projectExpenses", "labourLogs", "labourLogs.projectLabour"})
    Page<DailyLog> findByJobCard_Project_IdOrderByLogDateDesc(Long projectId, Pageable p);

    @Query("SELECT COALESCE(SUM(d.workValue),0) FROM DailyLog d WHERE d.jobCard.project.id=:pid")
    BigDecimal totalWorkValueByProject(@Param("pid") Long projectId);

    @EntityGraph(attributePaths = {"jobCard", "jobCard.project", "loggedBy", "projectExpenses", "labourLogs", "labourLogs.projectLabour"})
    @Query("SELECT d FROM DailyLog d WHERE d.jobCard.project.id=:pid AND d.logDate BETWEEN :from AND :to ORDER BY d.logDate ASC")
    List<DailyLog> findByProjectAndDateRange(@Param("pid") Long pid, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
