package com.supererp.erp.entity;

import com.supererp.erp.enums.EnquiryStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "enquiries")
@Data @NoArgsConstructor @AllArgsConstructor @lombok.experimental.SuperBuilder
public class Enquiry extends TenantAwareEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(length = 100)
    private String service;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnquiryStatus status = EnquiryStatus.NEW;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @Column(updatable = false)
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { submittedAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}

