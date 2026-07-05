package com.supererp.erp.controller;

import com.supererp.erp.entity.DocumentExpiryAlert;
import com.supererp.erp.rbac.Permissions;
import com.supererp.erp.rbac.annotation.RequiresPermission;
import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.service.DocumentExpiryAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Document Expiry Alert operations.
 * Provides endpoints for managing document expiry alerts,
 * acknowledgments, and renewals.
 */
@RestController
@RequestMapping("/api/document-expiry-alerts")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("DMS")
public class DocumentExpiryAlertController {

    private final DocumentExpiryAlertService alertService;

    /**
     * Create expiry alert for a document
     */
    @PostMapping("/documents/{documentId}")
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_MANAGE)
    public ResponseEntity<DocumentExpiryAlert> createAlert(
            @PathVariable Long documentId,
            @RequestBody DocumentExpiryAlert alert) {
        DocumentExpiryAlert created = alertService.createAlert(documentId, alert);
        return ResponseEntity.ok(created);
    }

    /**
     * Update an existing alert
     */
    @PutMapping("/{alertId}")
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_MANAGE)
    public ResponseEntity<DocumentExpiryAlert> updateAlert(
            @PathVariable Long alertId,
            @RequestBody DocumentExpiryAlert alert) {
        DocumentExpiryAlert updated = alertService.updateAlert(alertId, alert);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get alert by ID
     */
    @GetMapping("/{alertId}")
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_VIEW)
    public ResponseEntity<DocumentExpiryAlert> getAlertById(@PathVariable Long alertId) {
        return alertService.getAlertById(alertId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all alerts with pagination
     */
    @GetMapping
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_VIEW)
    public ResponseEntity<Page<DocumentExpiryAlert>> getAllAlerts(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DocumentExpiryAlert> alerts = alertService.getAllAlerts(pageable);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get alerts by status
     */
    @GetMapping("/status/{status}")
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_VIEW)
    public ResponseEntity<List<DocumentExpiryAlert>> getAlertsByStatus(@PathVariable String status) {
        List<DocumentExpiryAlert> alerts = alertService.getAlertsByStatus(status);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get alerts for a specific document
     */
    @GetMapping("/documents/{documentId}")
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_VIEW)
    public ResponseEntity<List<DocumentExpiryAlert>> getDocumentAlerts(@PathVariable Long documentId) {
        List<DocumentExpiryAlert> alerts = alertService.getDocumentAlerts(documentId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get documents expiring within a date range
     */
    @GetMapping("/expiring")
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_VIEW)
    public ResponseEntity<List<DocumentExpiryAlert>> getExpiringDocuments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DocumentExpiryAlert> alerts = alertService.getExpiringDocuments(startDate, endDate);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get upcoming expiries for the next N days
     */
    @GetMapping("/upcoming")
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_VIEW)
    public ResponseEntity<List<DocumentExpiryAlert>> getUpcomingExpiries(
            @RequestParam(defaultValue = "30") int days) {
        List<DocumentExpiryAlert> alerts = alertService.getUpcomingExpiries(days);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Acknowledge an alert
     */
    @PostMapping("/{alertId}/acknowledge")
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_MANAGE)
    public ResponseEntity<DocumentExpiryAlert> acknowledgeAlert(
            @PathVariable Long alertId,
            @RequestParam String acknowledgedBy) {
        DocumentExpiryAlert acknowledged = alertService.acknowledgeAlert(alertId, acknowledgedBy);
        return ResponseEntity.ok(acknowledged);
    }

    /**
     * Resolve an alert
     */
    @PostMapping("/{alertId}/resolve")
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_MANAGE)
    public ResponseEntity<DocumentExpiryAlert> resolveAlert(
            @PathVariable Long alertId,
            @RequestParam(required = false) String resolutionNotes,
            @RequestParam(required = false) Long renewalDocumentId) {
        DocumentExpiryAlert resolved = alertService.resolveAlert(alertId, resolutionNotes, renewalDocumentId);
        return ResponseEntity.ok(resolved);
    }

    /**
     * Delete an alert
     */
    @DeleteMapping("/{alertId}")
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_MANAGE)
    public ResponseEntity<Void> deleteAlert(@PathVariable Long alertId) {
        alertService.deleteAlert(alertId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get alert statistics
     */
    @GetMapping("/statistics")
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_VIEW)
    public ResponseEntity<Map<String, Long>> getStatistics() {
        return ResponseEntity.ok(alertService.getAlertStatistics());
    }
}
