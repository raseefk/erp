package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Application-level company settings.
 * There is exactly one row in this table in single-tenant mode.
 * tenant_id is kept for schema compatibility.
 */
@Entity
@Table(name = "company_settings")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Kept for schema compatibility — always AppTenantConfig.APP_TENANT_ID.
     */
    @Column(name = "tenant_id", nullable = false, unique = true, updatable = false)
    private UUID tenantId;

    @Column(length = 200)
    private String companyName;

    @Column(length = 200)
    private String tagline;

    @Column(length = 500)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String website;

    @Column(length = 50)
    private String taxNumber;

    @Column(nullable = false)
    private Integer defaultSickLeavesPerYear = 10;

    @Column(nullable = false)
    private Integer defaultCasualLeavesPerYear = 10;

    @Column(length = 200)
    private String weeklyOffDays = "SUNDAY";

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (tenantId == null) {
            tenantId = com.supererp.erp.config.AppTenantConfig.APP_TENANT_ID;
        }
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public java.util.List<java.time.DayOfWeek> getWeeklyOffDaysList() {
        if (weeklyOffDays == null || weeklyOffDays.isBlank()) return java.util.Collections.emptyList();
        return java.util.Arrays.stream(weeklyOffDays.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(java.time.DayOfWeek::valueOf)
                .collect(java.util.stream.Collectors.toList());
    }
}
