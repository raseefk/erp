package com.supererp.erp.repository;

import com.supererp.erp.entity.PayrollArrear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PayrollArrearRepository extends JpaRepository<PayrollArrear, Long> {

    @Query("SELECT a FROM PayrollArrear a JOIN FETCH a.employee WHERE a.employee.id = :empId AND a.paid = false ORDER BY a.createdAt ASC")
    List<PayrollArrear> findUnpaidByEmployee(@Param("empId") Long empId);

    @Query("SELECT a FROM PayrollArrear a JOIN FETCH a.employee WHERE a.paid = false ORDER BY a.employee.name ASC, a.createdAt ASC")
    List<PayrollArrear> findAllUnpaid();

    @Query("SELECT COALESCE(SUM(a.arrearAmount), 0) FROM PayrollArrear a WHERE a.employee.id = :empId AND a.paid = false")
    BigDecimal sumUnpaidByEmployee(@Param("empId") Long empId);
}
