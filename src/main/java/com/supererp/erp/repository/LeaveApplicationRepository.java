package com.supererp.erp.repository;

import com.supererp.erp.entity.LeaveApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"employee"})
    List<LeaveApplication> findByEmployeeIdOrderByStartDateDesc(Long employeeId);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"employee"})
    org.springframework.data.domain.Page<LeaveApplication> findAll(org.springframework.data.domain.Pageable p);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"employee"})
    List<LeaveApplication> findAllByOrderByAppliedAtDesc();

    @org.springframework.data.jpa.repository.Query("SELECT l FROM LeaveApplication l WHERE (:employeeId IS NULL OR l.employee.id = :employeeId) AND l.status = 'APPROVED' AND l.startDate <= :endDate AND l.endDate >= :startDate")
    List<LeaveApplication> findApprovedLeavesInPeriod(@org.springframework.data.repository.query.Param("employeeId") Long employeeId, @org.springframework.data.repository.query.Param("startDate") java.time.LocalDate startDate, @org.springframework.data.repository.query.Param("endDate") java.time.LocalDate endDate);
}
