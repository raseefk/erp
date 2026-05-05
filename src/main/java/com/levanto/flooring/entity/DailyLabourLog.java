package com.levanto.flooring.entity;

import jakarta.persistence.*;
import lombok.*;

import com.levanto.flooring.enums.ProjectExpenseStatus;
import java.math.BigDecimal;

@Entity
@Table(name = "daily_labour_logs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyLabourLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_log_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private DailyLog dailyLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_labour_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ProjectLabour projectLabour;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal wagePaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectExpenseStatus status = ProjectExpenseStatus.NEW;
}
