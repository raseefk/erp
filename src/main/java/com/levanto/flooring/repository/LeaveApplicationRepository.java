package com.levanto.flooring.repository;

import com.levanto.flooring.entity.LeaveApplication;
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

    @org.springframework.data.jpa.repository.Query("SELECT l FROM LeaveApplication l WHERE l.employee.id = :employeeId AND l.status = 'APPROVED' AND l.startDate <= :endDate AND l.endDate >= :startDate")
    List<LeaveApplication> findApprovedLeavesInPeriod(Long employeeId, java.time.LocalDate startDate, java.time.LocalDate endDate);
}
