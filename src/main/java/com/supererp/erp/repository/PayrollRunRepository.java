package com.supererp.erp.repository;

import com.supererp.erp.entity.PayrollRun;
import com.supererp.erp.enums.PayrollRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {

    Optional<PayrollRun> findByPayMonthAndPayYear(int month, int year);

    @Query("SELECT r FROM PayrollRun r ORDER BY r.payYear DESC, r.payMonth DESC")
    Page<PayrollRun> findAllOrdered(Pageable pageable);

    List<PayrollRun> findByStatusOrderByPayYearDescPayMonthDesc(PayrollRunStatus status);
}
