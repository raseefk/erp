package com.levanto.flooring.entity;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "employees")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50)
    private String employeeCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 15)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(length = 100)
    private String designation;

    @Column(length = 100)
    private String department;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 20)
    private String aadhaarNumber;

    @Column(length = 20)
    private String panNumber;

    // --- Bank Details ---
    @Column(length = 150)
    private String bankName;

    @Column(length = 50)
    private String accountNumber;

    @Column(length = 20)
    private String ifscCode;

    @Column(precision = 12, scale = 2)
    private BigDecimal monthlySalary = BigDecimal.ZERO;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate joiningDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser appUser;

    @Column(nullable = false)
    private boolean active = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
