package com.supererp.erp.tenant;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 63)
    private String slug;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color", length = 20)
    @Builder.Default
    private String primaryColor = "#3b82f6";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(length = 50)
    @Builder.Default
    private String plan = "STANDARD";

    @Column(name = "max_users")
    @Builder.Default
    private int maxUsers = 10;

    @Column(name = "max_storage_gb")
    @Builder.Default
    private Double maxStorageGb = 5.0;

    @Column(name = "upload_size_bytes")
    @Builder.Default
    private Long uploadSizeBytes = 0L;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "expires_at")
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private OffsetDateTime expiresAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
