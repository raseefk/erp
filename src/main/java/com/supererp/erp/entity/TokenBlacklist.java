package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "token_blacklist")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TokenBlacklist {

    @Id
    @Column(length = 36)
    private String jti;

    @Column(name = "tenant_id")
    private java.util.UUID tenantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 255)
    private String reason;

    @Column(name = "blacklisted_at")
    private OffsetDateTime blacklistedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @PrePersist
    void onCreate() { blacklistedAt = OffsetDateTime.now(); }
}
