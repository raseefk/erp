package com.supererp.erp.repository;

import com.supererp.erp.entity.PayrollConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollConfigRepository extends JpaRepository<PayrollConfig, Long> {

    @Query("SELECT c FROM PayrollConfig c JOIN FETCH c.employee WHERE c.employee.id = :empId")
    Optional<PayrollConfig> findByEmployeeId(@Param("empId") Long empId);
}
