package com.supererp.erp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supererp.erp.config.AppTenantConfig;
import com.supererp.erp.entity.Document;
import com.supererp.erp.entity.DocumentExpiryAlert;
import com.supererp.erp.repository.DocumentExpiryAlertRepository;
import com.supererp.erp.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing document expiry alerts.
 * Handles alert scheduling, notifications, and renewal tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentExpiryAlertService {

    private final DocumentExpiryAlertRepository alertRepo;
    private final DocumentRepository documentRepo;
    private final ObjectMapper objectMapper;

    /**
     * Create an expiry alert for a document
     */
    @Transactional
    public DocumentExpiryAlert createAlert(Long documentId, DocumentExpiryAlert alert) {
        Document document = documentRepo.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // Prevent duplicate active alerts
        if (alertRepo.existsByDocumentIdAndAlertStatus(documentId, "PENDING")) {
            throw new IllegalStateException("An active alert already exists for document: " + documentId);
        }

        alert.setDocument(document);
        alert.setAlertStatus("PENDING");
        // tenantId set automatically by @PrePersist in TenantAwareEntity

        // Calculate the date to send the first alert
        LocalDate scheduledDate = alert.getExpiryDate().minusDays(
            alert.getAlertDaysBefore() != null ? alert.getAlertDaysBefore() : 30);
        alert.setScheduledAlertDate(scheduledDate);

        DocumentExpiryAlert saved = alertRepo.save(alert);
        log.info("Created expiry alert {} for document {} expiring on {}",
            saved.getId(), documentId, alert.getExpiryDate());
        return saved;
    }

    /**
     * Update an existing alert
     */
    @Transactional
    public DocumentExpiryAlert updateAlert(Long alertId, DocumentExpiryAlert updated) {
        DocumentExpiryAlert existing = alertRepo.findByIdAndTenantId(alertId, AppTenantConfig.APP_TENANT_ID)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        existing.setExpiryDate(updated.getExpiryDate());
        existing.setAlertDaysBefore(updated.getAlertDaysBefore());
        existing.setRecipients(updated.getRecipients());
        existing.setNotificationChannels(updated.getNotificationChannels());
        existing.setReminderFrequencyDays(updated.getReminderFrequencyDays());

        // Recalculate scheduled date
        LocalDate scheduledDate = updated.getExpiryDate().minusDays(
            updated.getAlertDaysBefore() != null ? updated.getAlertDaysBefore() : 30);
        existing.setScheduledAlertDate(scheduledDate);

        return alertRepo.save(existing);
    }

    /**
     * Get alert by ID (tenant-scoped)
     */
    @Transactional(readOnly = true)
    public Optional<DocumentExpiryAlert> getAlertById(Long id) {
        return alertRepo.findByIdAndTenantId(id, AppTenantConfig.APP_TENANT_ID);
    }

    /**
     * Get all alerts paged, ordered by expiry date
     */
    @Transactional(readOnly = true)
    public Page<DocumentExpiryAlert> getAllAlerts(Pageable pageable) {
        return alertRepo.findByTenantIdOrderByExpiryDateAsc(AppTenantConfig.APP_TENANT_ID, pageable);
    }

    /**
     * Get alerts filtered by status
     */
    @Transactional(readOnly = true)
    public List<DocumentExpiryAlert> getAlertsByStatus(String status) {
        return alertRepo.findByTenantIdAndAlertStatus(AppTenantConfig.APP_TENANT_ID, status);
    }

    /**
     * Get active alerts for a specific document
     */
    @Transactional(readOnly = true)
    public List<DocumentExpiryAlert> getDocumentAlerts(Long documentId) {
        return alertRepo.findActiveAlertsByDocument(documentId);
    }

    /**
     * Get documents expiring within a date range
     */
    @Transactional(readOnly = true)
    public List<DocumentExpiryAlert> getExpiringDocuments(LocalDate startDate, LocalDate endDate) {
        return alertRepo.findExpiringSoon(startDate, endDate);
    }

    /**
     * Get upcoming expiries for the next N days
     */
    @Transactional(readOnly = true)
    public List<DocumentExpiryAlert> getUpcomingExpiries(int days) {
        return alertRepo.findExpiringSoon(LocalDate.now(), LocalDate.now().plusDays(days));
    }

    /**
     * Acknowledge an alert
     */
    @Transactional
    public DocumentExpiryAlert acknowledgeAlert(Long alertId, String acknowledgedBy) {
        DocumentExpiryAlert alert = alertRepo.findByIdAndTenantId(alertId, AppTenantConfig.APP_TENANT_ID)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        alert.setAlertStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(acknowledgedBy);
        alert.setAcknowledgedAt(LocalDateTime.now());

        DocumentExpiryAlert saved = alertRepo.save(alert);
        log.info("Alert {} acknowledged by {}", alertId, acknowledgedBy);
        return saved;
    }

    /**
     * Mark alert as resolved (e.g. document renewed)
     */
    @Transactional
    public DocumentExpiryAlert resolveAlert(Long alertId, String resolutionNotes, Long renewalDocumentId) {
        DocumentExpiryAlert alert = alertRepo.findByIdAndTenantId(alertId, AppTenantConfig.APP_TENANT_ID)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        alert.setAlertStatus("RESOLVED");
        alert.setResolutionNotes(resolutionNotes);
        alert.setRenewalDocumentId(renewalDocumentId);

        DocumentExpiryAlert saved = alertRepo.save(alert);
        log.info("Alert {} resolved", alertId);
        return saved;
    }

    /**
     * Delete an alert
     */
    @Transactional
    public void deleteAlert(Long alertId) {
        DocumentExpiryAlert alert = alertRepo.findByIdAndTenantId(alertId, AppTenantConfig.APP_TENANT_ID)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        alertRepo.delete(alert);
        log.info("Alert {} deleted", alertId);
    }

    /**
     * Scheduled job — runs daily at 8:00 AM to dispatch pending expiry alerts
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void checkAndSendExpiryAlerts() {
        log.info("Running expiry alert check...");
        LocalDate today = LocalDate.now();
        List<DocumentExpiryAlert> alertsToSend = alertRepo.findPendingAlertsToSend(today);

        for (DocumentExpiryAlert alert : alertsToSend) {
            try {
                sendAlertNotification(alert);
                alert.setAlertCount(alert.getAlertCount() + 1);

                if (alert.getFirstAlertSentAt() == null) {
                    alert.setFirstAlertSentAt(LocalDateTime.now());
                }
                alert.setLastAlertSentAt(LocalDateTime.now());
                alert.setAlertStatus("SENT");

                // Schedule next reminder
                int freq = alert.getReminderFrequencyDays() != null ? alert.getReminderFrequencyDays() : 7;
                alert.setScheduledAlertDate(today.plusDays(freq));

                alertRepo.save(alert);
                log.info("Sent expiry alert {} for document {}", alert.getId(), alert.getDocument().getId());
            } catch (Exception e) {
                log.error("Failed to send expiry alert {}: {}", alert.getId(), e.getMessage());
            }
        }
    }

    /**
     * Dispatch alert notification (pluggable — currently logs; wire up email/WhatsApp here)
     */
    private void sendAlertNotification(DocumentExpiryAlert alert) {
        List<String> recipients = parseRecipients(alert.getRecipients());
        String channels = alert.getNotificationChannels();
        log.info("EXPIRY ALERT — document '{}' expires {} — sending via {} to {}",
            alert.getDocument().getTitle(), alert.getExpiryDate(), channels, recipients);
        // TODO: integrate with EmailService / SmsService / WhatsAppService
    }

    private List<String> parseRecipients(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Could not parse recipients JSON: {}", json);
            return List.of();
        }
    }

    /**
     * Alert statistics for dashboard
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getAlertStatistics() {
        return Map.of(
            "pending",      alertRepo.countByTenantIdAndAlertStatus(AppTenantConfig.APP_TENANT_ID, "PENDING"),
            "sent",         alertRepo.countByTenantIdAndAlertStatus(AppTenantConfig.APP_TENANT_ID, "SENT"),
            "acknowledged", alertRepo.countByTenantIdAndAlertStatus(AppTenantConfig.APP_TENANT_ID, "ACKNOWLEDGED"),
            "resolved",     alertRepo.countByTenantIdAndAlertStatus(AppTenantConfig.APP_TENANT_ID, "RESOLVED")
        );
    }
}
