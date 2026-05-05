package com.supererp.erp.entity;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "employees")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee extends TenantAwareEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(length = 100)
    private String designation;

    @Column(length = 100)
    private String department;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal monthlySalary = BigDecimal.ZERO;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate joiningDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); }
}
