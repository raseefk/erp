package com.supererp.erp.repository;

import com.supererp.erp.entity.DigitalSignature;
import com.supererp.erp.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DigitalSignatureRepository extends JpaRepository<DigitalSignature, Long> {

    @Query("SELECT ds FROM DigitalSignature ds JOIN FETCH ds.document WHERE ds.document.id = :documentId ORDER BY ds.createdAt DESC")
    List<DigitalSignature> findByDocumentIdOrderByCreatedAtDesc(@Param("documentId") Long documentId);

    @Query("SELECT ds FROM DigitalSignature ds JOIN FETCH ds.document WHERE ds.signerEmail = :email AND ds.signatureStatus IN ('PENDING', 'SIGNED')")
    List<DigitalSignature> findActiveSignaturesByEmail(@Param("email") String email);

    @Query("SELECT ds FROM DigitalSignature ds JOIN FETCH ds.document WHERE ds.document.id = :documentId AND ds.signatureStatus = :status")
    List<DigitalSignature> findByDocumentIdAndStatus(
            @Param("documentId") Long documentId, @Param("status") String status);

    @Query("SELECT COUNT(ds) FROM DigitalSignature ds WHERE ds.document.id = :documentId AND ds.signatureStatus = 'SIGNED'")
    Long countSignedSignaturesByDocument(@Param("documentId") Long documentId);

    @Query("SELECT ds FROM DigitalSignature ds JOIN FETCH ds.document WHERE ds.validUntil < :date AND ds.signatureStatus = 'SIGNED'")
    List<DigitalSignature> findExpiredSignatures(@Param("date") LocalDateTime date);

    // Paginated list with document eagerly fetched
    @Query(value = "SELECT ds FROM DigitalSignature ds JOIN FETCH ds.document WHERE ds.tenantId = :tenantId ORDER BY ds.createdAt DESC",
           countQuery = "SELECT COUNT(ds) FROM DigitalSignature ds WHERE ds.tenantId = :tenantId")
    Page<DigitalSignature> findByTenantIdOrderByCreatedAtDesc(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query(value = "SELECT ds FROM DigitalSignature ds JOIN FETCH ds.document WHERE ds.tenantId = :tenantId AND ds.signatureStatus = :status ORDER BY ds.createdAt DESC",
           countQuery = "SELECT COUNT(ds) FROM DigitalSignature ds WHERE ds.tenantId = :tenantId AND ds.signatureStatus = :status")
    Page<DigitalSignature> findByTenantIdAndSignatureStatus(
            @Param("tenantId") UUID tenantId, @Param("status") String status, Pageable pageable);

    @Query("SELECT ds FROM DigitalSignature ds JOIN FETCH ds.document WHERE ds.id = :id AND ds.tenantId = :tenantId")
    Optional<DigitalSignature> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    boolean existsByDocumentIdAndSignerEmailAndSignatureStatusIn(
            Long documentId, String signerEmail, List<String> statuses);
}
