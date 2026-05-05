package com.levanto.flooring.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "vendors")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Vendor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String contactPerson;

    @Column(length = 15)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 20)
    private String gstNumber;

    @Column(length = 200)
    private String materialSupplied;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // --- Bank Details ---
    @Column(length = 200)
    private String bankAccountName;

    @Column(length = 50)
    private String bankAccountNumber;

    @Column(length = 200)
    private String bankName;

    @Column(length = 20)
    private String ifscCode;

    @Column(nullable = false)
    private boolean active = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
