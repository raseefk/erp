package com.supererp.erp.repository;

import com.supererp.erp.entity.DigitalSignature;
import com.supererp.erp.entity.Document;
import org.springframework.data.domain.Page;
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

    List<DigitalSignature> findByDocumentOrderByCreatedAtDesc(Document document);

    List<DigitalSignature> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    List<DigitalSignature> findBySignerEmailOrderByCreatedAtDesc(String signerEmail);

    List<DigitalSignature> findBySignerUserIdOrderByCreatedAtDesc(Long signerUserId);

    @Query("SELECT ds FROM DigitalSignature ds WHERE ds.document.id = :documentId AND ds.signatureStatus = :status")
    List<DigitalSignature> findByDocumentIdAndStatus(
            @Param("documentId") Long documentId, @Param("status") String status);

    @Query("SELECT COUNT(ds) FROM DigitalSignature ds WHERE ds.document.id = :documentId AND ds.signatureStatus = 'SIGNED'")
    Long countSignedSignaturesByDocument(@Param("documentId") Long documentId);

    @Query("SELECT ds FROM DigitalSignature ds WHERE ds.validUntil < :date AND ds.signatureStatus = 'SIGNED'")
    List<DigitalSignature> findExpiredSignatures(@Param("date") LocalDateTime date);

    @Query("SELECT ds FROM DigitalSignature ds WHERE ds.signerEmail = :email AND ds.signatureStatus IN ('PENDING', 'SIGNED')")
    List<DigitalSignature> findActiveSignaturesByEmail(@Param("email") String email);

    Page<DigitalSignature> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    @Query("SELECT ds FROM DigitalSignature ds WHERE ds.tenantId = :tenantId AND ds.signatureStatus = :status ORDER BY ds.createdAt DESC")
    Page<DigitalSignature> findByTenantIdAndSignatureStatus(
            @Param("tenantId") UUID tenantId, @Param("status") String status, Pageable pageable);

    Optional<DigitalSignature> findByIdAndTenantId(Long id, UUID tenantId);

    boolean existsByDocumentIdAndSignerEmailAndSignatureStatusIn(
            Long documentId, String signerEmail, List<String> statuses);
}
