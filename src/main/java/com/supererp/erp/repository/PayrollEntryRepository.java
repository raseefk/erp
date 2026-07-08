package com.supererp.erp.repository;

import com.supererp.erp.entity.PayrollEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollEntryRepository extends JpaRepository<PayrollEntry, Long> {

    @Query("SELECT e FROM PayrollEntry e JOIN FETCH e.employee WHERE e.payrollRun.id = :runId ORDER BY e.employee.name ASC")
    List<PayrollEntry> findByRunIdWithEmployee(@Param("runId") Long runId);

    @Query("SELECT e FROM PayrollEntry e JOIN FETCH e.employee WHERE e.employee.id = :empId ORDER BY e.payrollRun.payYear DESC, e.payrollRun.payMonth DESC")
    List<PayrollEntry> findByEmployeeIdOrdered(@Param("empId") Long empId);

    Optional<PayrollEntry> findByPayrollRunIdAndEmployeeId(Long runId, Long empId);
}
