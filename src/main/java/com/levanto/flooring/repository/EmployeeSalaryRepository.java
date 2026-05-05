package com.levanto.flooring.repository;

import com.levanto.flooring.entity.Employee;
import com.levanto.flooring.entity.EmployeeSalary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

public interface EmployeeSalaryRepository extends JpaRepository<EmployeeSalary, Long> {

    @EntityGraph(attributePaths = {"employee"})
    List<EmployeeSalary> findByEmployeeOrderBySalaryCreditedDateDesc(Employee employee);

    @EntityGraph(attributePaths = {"employee"})
    Optional<EmployeeSalary> findByEmployeeAndSalaryMonthYear(Employee employee, String monthYear);

    @EntityGraph(attributePaths = {"employee"})
    Page<EmployeeSalary> findAllByOrderBySalaryCreditedDateDesc(Pageable pageable);

    /** Total salary paid for P&L reporting */
    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM EmployeeSalary s WHERE s.salaryCreditedDate BETWEEN :from AND :to")
    BigDecimal sumSalaryByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @EntityGraph(attributePaths = {"employee"})
    @Query("SELECT s FROM EmployeeSalary s WHERE s.salaryCreditedDate BETWEEN :from AND :to ORDER BY s.salaryCreditedDate DESC")
    Page<EmployeeSalary> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);
}
