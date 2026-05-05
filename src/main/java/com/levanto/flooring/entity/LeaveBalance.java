package com.levanto.flooring.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "leave_balances", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "year"})
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveBalance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, name = "year_val")
    private Integer year;

    @Column(nullable = false)
    private Integer allocatedSickLeaves = 0;

    @Column(nullable = false)
    private Integer usedSickLeaves = 0;

    @Column(nullable = false)
    private Integer allocatedCasualLeaves = 0;

    @Column(nullable = false)
    private Integer usedCasualLeaves = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    
    public int getRemainingSickLeaves() {
        return allocatedSickLeaves - usedSickLeaves;
    }
    
    public int getRemainingCasualLeaves() {
        return allocatedCasualLeaves - usedCasualLeaves;
    }
}
