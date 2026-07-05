package com.supererp.erp.repository;

import com.supererp.erp.entity.Document;
import com.supererp.erp.entity.DocumentExpiryAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentExpiryAlertRepository extends JpaRepository<DocumentExpiryAlert, Long> {

    @Query("SELECT dea FROM DocumentExpiryAlert dea JOIN FETCH dea.document WHERE dea.document.id = :documentId ORDER BY dea.createdAt DESC")
    List<DocumentExpiryAlert> findByDocumentIdOrderByCreatedAtDesc(@Param("documentId") Long documentId);

    @Query(value = "SELECT dea FROM DocumentExpiryAlert dea JOIN FETCH dea.document WHERE dea.tenantId = :tenantId ORDER BY dea.expiryDate ASC",
           countQuery = "SELECT COUNT(dea) FROM DocumentExpiryAlert dea WHERE dea.tenantId = :tenantId")
    Page<DocumentExpiryAlert> findByTenantIdOrderByExpiryDateAsc(
            @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT dea FROM DocumentExpiryAlert dea JOIN FETCH dea.document WHERE dea.tenantId = :tenantId AND dea.alertStatus = :status ORDER BY dea.expiryDate ASC")
    List<DocumentExpiryAlert> findByTenantIdAndAlertStatus(
            @Param("tenantId") UUID tenantId, @Param("status") String status);

    @Query("SELECT dea FROM DocumentExpiryAlert dea JOIN FETCH dea.document WHERE dea.scheduledAlertDate <= :date AND dea.alertStatus = 'PENDING'")
    List<DocumentExpiryAlert> findPendingAlertsToSend(@Param("date") LocalDate date);

    @Query("SELECT dea FROM DocumentExpiryAlert dea JOIN FETCH dea.document WHERE dea.expiryDate BETWEEN :startDate AND :endDate AND dea.alertStatus IN ('PENDING', 'SENT') ORDER BY dea.expiryDate ASC")
    List<DocumentExpiryAlert> findExpiringSoon(
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(dea) FROM DocumentExpiryAlert dea WHERE dea.tenantId = :tenantId AND dea.alertStatus = :status")
    Long countByTenantIdAndAlertStatus(
            @Param("tenantId") UUID tenantId, @Param("status") String status);

    @Query("SELECT dea FROM DocumentExpiryAlert dea JOIN FETCH dea.document WHERE dea.id = :id AND dea.tenantId = :tenantId")
    Optional<DocumentExpiryAlert> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    boolean existsByDocumentIdAndAlertStatus(Long documentId, String alertStatus);

    @Query("SELECT dea FROM DocumentExpiryAlert dea JOIN FETCH dea.document WHERE dea.document.id = :documentId AND dea.alertStatus IN ('PENDING', 'SENT') ORDER BY dea.expiryDate DESC")
    List<DocumentExpiryAlert> findActiveAlertsByDocument(@Param("documentId") Long documentId);
}
