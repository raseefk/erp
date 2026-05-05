package com.supererp.erp.entity;

import com.supererp.erp.enums.JobCardStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_cards")
@Data @NoArgsConstructor @AllArgsConstructor @lombok.experimental.SuperBuilder
public class JobCard extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String phase;           // e.g. Foundation, Tiling, Skirting

    @Column(columnDefinition = "TEXT")
    private String description;

    // Assigned site engineer (Employee)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    private Employee assignedEngineer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobCardStatus status = JobCardStatus.PLANNED;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate targetDate;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "jobCard", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DailyLog> dailyLogs = new ArrayList<>();

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}

