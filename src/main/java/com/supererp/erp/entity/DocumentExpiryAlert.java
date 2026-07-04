package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Document expiry alert for tracking document expiration.
 * Used for licenses, contracts, insurance policies, etc.
 */
@Entity
@Table(name = "document_expiry_alerts")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DocumentExpiryAlert extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "alert_type", nullable = false, length = 50)
    @Builder.Default
    private String alertType = "EXPIRY"; // EXPIRY, RENEWAL, REVIEW

    @Column(name = "alert_status", nullable = false, length = 50)
    @Builder.Default
    private String alertStatus = "PENDING"; // PENDING, SENT, ACKNOWLEDGED, RESOLVED

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "alert_days_before")
    @Builder.Default
    private Integer alertDaysBefore = 30;

    @Column(name = "scheduled_alert_date")
    private LocalDate scheduledAlertDate;

    @Column(name = "first_alert_sent_at")
    private LocalDateTime firstAlertSentAt;

    @Column(name = "last_alert_sent_at")
    private LocalDateTime lastAlertSentAt;

    @Column(name = "alert_count")
    @Builder.Default
    private Integer alertCount = 0;

    @Column(name = "recipients", columnDefinition = "TEXT")
    private String recipients; // JSON array of email addresses

    @Column(name = "notification_channels", length = 200)
    @Builder.Default
    private String notificationChannels = "EMAIL,IN_APP"; // EMAIL, SMS, IN_APP, WHATSAPP

    @Column(name = "reminder_frequency_days")
    @Builder.Default
    private Integer reminderFrequencyDays = 7; // Remind every X days after first alert

    @Column(name = "acknowledged_by", length = 200)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "renewal_document_id")
    private Long renewalDocumentId; // Link to renewed document

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
