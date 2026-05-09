package com.supererp.erp.repository;

import com.supererp.erp.entity.AdvancePayment;
import com.supererp.erp.enums.AdvancePaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdvancePaymentRepository extends JpaRepository<AdvancePayment, Long> {

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"project"})
    Optional<AdvancePayment> findById(Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"project"})
    Page<AdvancePayment> findAllByOrderByDateDesc(Pageable pageable);

    @Query("SELECT MAX(CAST(SUBSTRING(a.advanceNumber, 5) AS int)) FROM AdvancePayment a WHERE a.advanceNumber LIKE 'ADV-%'")
    Optional<Integer> findMaxAdvanceNumber();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"project"})
    List<AdvancePayment> findByStatusOrderByDateDesc(AdvancePaymentStatus status);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"project"})
    List<AdvancePayment> findByProject_IdAndStatusOrderByDateDesc(Long projectId, AdvancePaymentStatus status);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"project"})
    List<AdvancePayment> findByProject_IdOrderByDateDesc(Long projectId);
}
