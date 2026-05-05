package com.levanto.flooring.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "company_settings")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanySettings {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String companyName;

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

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public java.util.List<java.time.DayOfWeek> getWeeklyOffDaysList() {
        if (weeklyOffDays == null || weeklyOffDays.isBlank()) return java.util.Collections.emptyList();
        return java.util.Arrays.stream(weeklyOffDays.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(java.time.DayOfWeek::valueOf)
                .collect(java.util.stream.Collectors.toList());
    }
}
