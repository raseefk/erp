package com.levanto.flooring.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "leave_applications")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveType leaveType;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(updatable = false)
    private LocalDateTime appliedAt;

    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { appliedAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum LeaveType {
        SICK_LEAVE, CASUAL_LEAVE, LOSS_OF_PAY
    }

    public enum LeaveStatus {
        PENDING, APPROVED, REJECTED
    }
}
