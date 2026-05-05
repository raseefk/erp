package com.levanto.flooring.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "customers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(nullable = false, length = 200)
    private String name;

    @NotBlank @Column(nullable = false, length = 15)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 20)
    private String gstNumber;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
