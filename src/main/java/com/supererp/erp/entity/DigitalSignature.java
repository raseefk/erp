package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

/**
 * Digital signature record for documents.
 * Stores signature metadata, verification status, and audit trail.
 */
@Entity
@Table(name = "digital_signatures")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DigitalSignature extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_version_id")
    private DocumentVersion documentVersion;

    @Column(name = "signer_name", nullable = false, length = 200)
    private String signerName;

    @Column(name = "signer_email", nullable = false, length = 255)
    private String signerEmail;

    @Column(name = "signer_designation", length = 200)
    private String signerDesignation;

    @Column(name = "signer_user_id")
    private Long signerUserId;

    @Column(name = "signature_type", nullable = false, length = 50)
    @Builder.Default
    private String signatureType = "ELECTRONIC"; // ELECTRONIC, DIGITAL_CERTIFICATE, BIOMETRIC

    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData; // Base64 encoded signature image or certificate data

    @Column(name = "signature_hash", length = 512)
    private String signatureHash; // Hash of signed content for verification

    @Column(name = "certificate_serial", length = 100)
    private String certificateSerial; // For digital certificates

    @Column(name = "certificate_issuer", length = 255)
    private String certificateIssuer;

    @Column(name = "signature_status", nullable = false, length = 50)
    @Builder.Default
    private String signatureStatus = "PENDING"; // PENDING, SIGNED, VERIFIED, REJECTED, EXPIRED

    @Column(name = "signature_purpose", length = 500)
    private String signaturePurpose; // Why document was signed

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "geo_location", length = 200)
    private String geoLocation; // GPS coordinates if available

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by", length = 200)
    private String verifiedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "valid_until")
    private LocalDateTime validUntil; // Signature validity expiry

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
