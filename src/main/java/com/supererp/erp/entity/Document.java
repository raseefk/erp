package com.supererp.erp.entity;

import com.supererp.erp.enums.DocumentAccessLevel;
import com.supererp.erp.enums.DocumentCategory;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Central document record in the Document Management System.
 * Supports version control, expiry alerts, and role-based access.
 */
@Entity
@Table(name = "documents")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Document extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DocumentCategory category = DocumentCategory.OTHER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private DocumentFolder folder;

    @Column(name = "current_version", nullable = false)
    @Builder.Default
    private Integer currentVersion = 1;

    /** Tags for search/filter */
    @Column(length = 500)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    @Builder.Default
    private DocumentAccessLevel accessLevel = DocumentAccessLevel.PUBLIC;

    /** Department restriction if access_level = DEPARTMENT */
    @Column(name = "allowed_department", length = 100)
    private String allowedDepartment;

    /** Role restriction if access_level = ROLE */
    @Column(name = "allowed_role", length = 100)
    private String allowedRole;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "expiry_alert_days")
    @Builder.Default
    private Integer expiryAlertDays = 30;

    @Column(name = "alert_sent")
    @Builder.Default
    private Boolean alertSent = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_by", length = 200)
    private String createdBy;

    /** Link to linked entity (optional) — e.g., vendor/employee */
    @Column(name = "linked_entity_type", length = 50)
    private String linkedEntityType; // VENDOR, EMPLOYEE, PROJECT, etc.

    @Column(name = "linked_entity_id")
    private Long linkedEntityId;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentVersion> versions = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
