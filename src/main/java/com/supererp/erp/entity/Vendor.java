package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "vendors")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Vendor extends TenantAwareEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "contact_person", length = 200)
    private String contactPerson;

    @Column(length = 30)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "material_supplied", length = 200)
    private String materialSupplied;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_account_name", length = 200)
    private String bankAccountName;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); }
}
