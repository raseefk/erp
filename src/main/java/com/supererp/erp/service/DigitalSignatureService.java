package com.supererp.erp.service;

import com.supererp.erp.entity.DigitalSignature;
import com.supererp.erp.entity.Document;
import com.supererp.erp.repository.DigitalSignatureRepository;
import com.supererp.erp.repository.DocumentRepository;
import com.supererp.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing digital signatures on documents.
 * Supports electronic signatures, digital certificate signatures,
 * and signature verification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalSignatureService {

    private final DigitalSignatureRepository signatureRepo;
    private final DocumentRepository documentRepo;

    /**
     * Create a signature request for a document
     */
    @Transactional
    public DigitalSignature createSignatureRequest(Long documentId, DigitalSignature signature) {
        Document document = documentRepo.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // Check if already signed by this signer
        if (signatureRepo.existsByDocumentIdAndSignerEmailAndSignatureStatusIn(
                documentId, signature.getSignerEmail(),
                List.of("SIGNED", "VERIFIED"))) {
            throw new IllegalStateException("Document already signed by: " + signature.getSignerEmail());
        }

        signature.setDocument(document);
        signature.setSignatureStatus("PENDING");
        // tenantId is set automatically by @PrePersist on TenantAwareEntity

        DigitalSignature saved = signatureRepo.save(signature);
        log.info("Created signature request {} for document {} signer {}",
            saved.getId(), documentId, signature.getSignerEmail());
        return saved;
    }

    /**
     * Create multiple signature requests for a document
     */
    @Transactional
    public List<DigitalSignature> createMultipleSignatureRequests(Long documentId,
            List<DigitalSignature> signatures) {
        return signatures.stream()
            .map(sig -> createSignatureRequest(documentId, sig))
            .toList();
    }

    /**
     * Sign a document
     */
    @Transactional
    public DigitalSignature signDocument(Long signatureId, String signatureData,
                                          String ipAddress, String userAgent, String geoLocation) {
        UUID tenantId = TenantContext.getTenantId();
        DigitalSignature signature = signatureRepo.findByIdAndTenantId(signatureId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Signature not found: " + signatureId));

        if (!"PENDING".equals(signature.getSignatureStatus())) {
            throw new IllegalStateException("Signature is not in PENDING status: " + signature.getSignatureStatus());
        }

        // Generate hash of the document content for integrity verification
        String contentHash = generateContentHash(signature.getDocument());

        signature.setSignatureData(signatureData);
        signature.setSignatureHash(contentHash);
        signature.setSignatureStatus("SIGNED");
        signature.setIpAddress(ipAddress);
        signature.setUserAgent(userAgent);
        signature.setGeoLocation(geoLocation);
        signature.setSignedAt(LocalDateTime.now());

        DigitalSignature saved = signatureRepo.save(signature);
        log.info("Document {} signed by {}", signature.getDocument().getId(), signature.getSignerEmail());
        return saved;
    }

    /**
     * Verify a signature — recalculates content hash and compares
     */
    @Transactional
    public DigitalSignature verifySignature(Long signatureId, String verifiedBy) {
        UUID tenantId = TenantContext.getTenantId();
        DigitalSignature signature = signatureRepo.findByIdAndTenantId(signatureId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Signature not found: " + signatureId));

        if (!"SIGNED".equals(signature.getSignatureStatus())) {
            throw new IllegalStateException("Signature must be SIGNED before verification");
        }

        // Verify content hash matches current document state
        String currentHash = generateContentHash(signature.getDocument());
        if (!currentHash.equals(signature.getSignatureHash())) {
            log.warn("Signature verification FAILED — content hash mismatch for signature {}", signatureId);
            signature.setSignatureStatus("REJECTED");
            signature.setRejectionReason("Document content has been modified since signing");
            return signatureRepo.save(signature);
        }

        signature.setSignatureStatus("VERIFIED");
        signature.setVerifiedAt(LocalDateTime.now());
        signature.setVerifiedBy(verifiedBy);

        DigitalSignature saved = signatureRepo.save(signature);
        log.info("Signature {} verified by {}", signatureId, verifiedBy);
        return saved;
    }

    /**
     * Reject a signature
     */
    @Transactional
    public DigitalSignature rejectSignature(Long signatureId, String rejectionReason) {
        UUID tenantId = TenantContext.getTenantId();
        DigitalSignature signature = signatureRepo.findByIdAndTenantId(signatureId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Signature not found: " + signatureId));

        signature.setSignatureStatus("REJECTED");
        signature.setRejectionReason(rejectionReason);

        DigitalSignature saved = signatureRepo.save(signature);
        log.info("Signature {} rejected: {}", signatureId, rejectionReason);
        return saved;
    }

    /**
     * Get all signatures for a document
     */
    @Transactional(readOnly = true)
    public List<DigitalSignature> getDocumentSignatures(Long documentId) {
        return signatureRepo.findByDocumentIdOrderByCreatedAtDesc(documentId);
    }

    /**
     * Get signature by ID
     */
    @Transactional(readOnly = true)
    public Optional<DigitalSignature> getSignatureById(Long id) {
        return signatureRepo.findByIdAndTenantId(id, TenantContext.getTenantId());
    }

    /**
     * Get all signatures with pagination
     */
    @Transactional(readOnly = true)
    public Page<DigitalSignature> getAllSignatures(Pageable pageable) {
        return signatureRepo.findByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId(), pageable);
    }

    /**
     * Get signatures filtered by status
     */
    @Transactional(readOnly = true)
    public Page<DigitalSignature> getSignaturesByStatus(String status, Pageable pageable) {
        return signatureRepo.findByTenantIdAndSignatureStatus(
                TenantContext.getTenantId(), status, pageable);
    }

    /**
     * Get pending/signed signatures for a specific signer email
     */
    @Transactional(readOnly = true)
    public List<DigitalSignature> getSignaturesForUser(String email) {
        return signatureRepo.findActiveSignaturesByEmail(email);
    }
    /**
     * Check if document has at least requiredSignatures signed signatures
     */
    @Transactional(readOnly = true)
    public boolean isDocumentFullySigned(Long documentId, int requiredSignatures) {
        Long signedCount = signatureRepo.countSignedSignaturesByDocument(documentId);
        return signedCount >= requiredSignatures;
    }

    /**
     * Get signature statistics for dashboard
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getSignatureStatistics() {
        UUID tenantId = TenantContext.getTenantId();
        return Map.of(
            "pending",  signatureRepo.findByTenantIdAndSignatureStatus(tenantId, "PENDING",  Pageable.unpaged()).getTotalElements(),
            "signed",   signatureRepo.findByTenantIdAndSignatureStatus(tenantId, "SIGNED",   Pageable.unpaged()).getTotalElements(),
            "verified", signatureRepo.findByTenantIdAndSignatureStatus(tenantId, "VERIFIED", Pageable.unpaged()).getTotalElements(),
            "rejected", signatureRepo.findByTenantIdAndSignatureStatus(tenantId, "REJECTED", Pageable.unpaged()).getTotalElements()
        );
    }

    /**
     * Generate a reproducible content hash from document identity.
     * Used for tamper-detection when verifying signatures.
     */
    private String generateContentHash(Document document) {
        String hashInput = String.format("%d|%s|%d|%s",
            document.getId(),
            document.getTitle(),
            document.getCurrentVersion(),
            document.getUpdatedAt() != null ? document.getUpdatedAt().toString() : "");
        return DigestUtils.md5DigestAsHex(hashInput.getBytes(StandardCharsets.UTF_8));
    }
}
