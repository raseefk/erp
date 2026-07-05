package com.supererp.erp.controller;

import com.supererp.erp.entity.DigitalSignature;
import com.supererp.erp.rbac.Permissions;
import com.supererp.erp.rbac.annotation.RequiresPermission;
import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.service.DigitalSignatureService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Digital Signature operations.
 * Provides endpoints for signing documents, verifying signatures,
 * and managing signature requests.
 */
@RestController
@RequestMapping("/api/digital-signatures")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("DMS")
public class DigitalSignatureController {

    private final DigitalSignatureService signatureService;

    /**
     * Create a signature request for a document
     */
    @PostMapping("/documents/{documentId}/request")
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_SIGN)
    public ResponseEntity<DigitalSignature> createSignatureRequest(
            @PathVariable Long documentId,
            @RequestBody DigitalSignature signature) {
        DigitalSignature created = signatureService.createSignatureRequest(documentId, signature);
        return ResponseEntity.ok(created);
    }

    /**
     * Create multiple signature requests for a document
     */
    @PostMapping("/documents/{documentId}/request-multiple")
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_SIGN)
    public ResponseEntity<List<DigitalSignature>> createMultipleSignatureRequests(
            @PathVariable Long documentId,
            @RequestBody List<DigitalSignature> signatures) {
        List<DigitalSignature> created = signatureService.createMultipleSignatureRequests(documentId, signatures);
        return ResponseEntity.ok(created);
    }

    /**
     * Sign a document
     */
    @PostMapping("/{signatureId}/sign")
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_SIGN)
    public ResponseEntity<DigitalSignature> signDocument(
            @PathVariable Long signatureId,
            @RequestBody SignRequest signRequest,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        DigitalSignature signed = signatureService.signDocument(
            signatureId, 
            signRequest.getSignatureData(),
            ipAddress,
            userAgent,
            signRequest.getGeoLocation()
        );
        return ResponseEntity.ok(signed);
    }

    /**
     * Verify a signature
     */
    @PostMapping("/{signatureId}/verify")
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_VERIFY)
    public ResponseEntity<DigitalSignature> verifySignature(
            @PathVariable Long signatureId,
            @RequestParam String verifiedBy) {
        DigitalSignature verified = signatureService.verifySignature(signatureId, verifiedBy);
        return ResponseEntity.ok(verified);
    }

    /**
     * Reject a signature
     */
    @PostMapping("/{signatureId}/reject")
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_VERIFY)
    public ResponseEntity<DigitalSignature> rejectSignature(
            @PathVariable Long signatureId,
            @RequestParam String reason) {
        DigitalSignature rejected = signatureService.rejectSignature(signatureId, reason);
        return ResponseEntity.ok(rejected);
    }

    /**
     * Get all signatures for a document
     */
    @GetMapping("/documents/{documentId}")
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_VIEW)
    public ResponseEntity<List<DigitalSignature>> getDocumentSignatures(@PathVariable Long documentId) {
        List<DigitalSignature> signatures = signatureService.getDocumentSignatures(documentId);
        return ResponseEntity.ok(signatures);
    }

    /**
     * Get signature by ID
     */
    @GetMapping("/{signatureId}")
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_VIEW)
    public ResponseEntity<DigitalSignature> getSignatureById(@PathVariable Long signatureId) {
        return signatureService.getSignatureById(signatureId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all signatures with pagination
     */
    @GetMapping
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_VIEW)
    public ResponseEntity<Page<DigitalSignature>> getAllSignatures(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DigitalSignature> signatures = signatureService.getAllSignatures(pageable);
        return ResponseEntity.ok(signatures);
    }

    /**
     * Get signatures by status
     */
    @GetMapping("/status/{status}")
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_VIEW)
    public ResponseEntity<Page<DigitalSignature>> getSignaturesByStatus(
            @PathVariable String status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DigitalSignature> signatures = signatureService.getSignaturesByStatus(status, pageable);
        return ResponseEntity.ok(signatures);
    }

    /**
     * Get signatures for current user
     */
    @GetMapping("/my-signatures")
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_VIEW)
    public ResponseEntity<List<DigitalSignature>> getMySignatures(@RequestParam String email) {
        List<DigitalSignature> signatures = signatureService.getSignaturesForUser(email);
        return ResponseEntity.ok(signatures);
    }

    /**
     * Check if document is fully signed
     */
    @GetMapping("/documents/{documentId}/status")
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_VIEW)
    public ResponseEntity<Map<String, Object>> getDocumentSignatureStatus(
            @PathVariable Long documentId,
            @RequestParam int requiredSignatures) {
        boolean fullySigned = signatureService.isDocumentFullySigned(documentId, requiredSignatures);
        return ResponseEntity.ok(Map.of(
            "documentId", documentId,
            "requiredSignatures", requiredSignatures,
            "fullySigned", fullySigned
        ));
    }

    /**
     * Get signature statistics
     */
    @GetMapping("/statistics")
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_VIEW)
    public ResponseEntity<Map<String, Long>> getStatistics() {
        return ResponseEntity.ok(signatureService.getSignatureStatistics());
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple proxies
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * DTO for sign request
     */
    @lombok.Data
    public static class SignRequest {
        private String signatureData;
        private String geoLocation;
    }
}
