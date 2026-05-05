package com.levanto.flooring.repository;

import com.levanto.flooring.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"employee"})
    List<Attendance> findByDateOrderByEmployeeNameAsc(LocalDate date);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"employee"})
    Optional<Attendance> findByEmployeeIdAndDate(Long employeeId, LocalDate date);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"employee"})
    List<Attendance> findByEmployeeIdOrderByDateDesc(Long employeeId);

    @Query("SELECT a FROM Attendance a WHERE a.date >= :startDate AND a.date <= :endDate AND (:employeeId IS NULL OR a.employee.id = :employeeId) ORDER BY a.date DESC, a.employee.name ASC")
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"employee"})
    List<Attendance> findByDateBetweenAndEmployeeIdOptional(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate, 
        @Param("employeeId") Long employeeId);
}
