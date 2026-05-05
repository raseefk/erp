package com.levanto.flooring.repository;

import com.levanto.flooring.entity.DailyLabourLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyLabourLogRepository extends JpaRepository<DailyLabourLog, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"dailyLog", "dailyLog.jobCard"})
    List<DailyLabourLog> findByProjectLabourIdOrderByDailyLogLogDateDesc(Long projectLabourId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"dailyLog", "dailyLog.jobCard", "projectLabour"})
    @org.springframework.data.jpa.repository.Query("SELECT l FROM DailyLabourLog l WHERE l.projectLabour.project.id = :projectId " +
           "AND l.status = 'APPROVED' " +
           "AND (cast(:fromDate as date) IS NULL OR l.dailyLog.logDate >= :fromDate) " +
           "AND (cast(:toDate as date) IS NULL OR l.dailyLog.logDate <= :toDate) " +
           "AND (:labourName IS NULL OR LOWER(l.projectLabour.name) LIKE LOWER(CONCAT('%', :labourName, '%'))) " +
           "ORDER BY l.projectLabour.name ASC, l.dailyLog.logDate ASC")
    List<DailyLabourLog> findApprovedFiltered(
            @org.springframework.data.repository.query.Param("projectId") Long projectId,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDate fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDate toDate,
            @org.springframework.data.repository.query.Param("labourName") String labourName);
}
