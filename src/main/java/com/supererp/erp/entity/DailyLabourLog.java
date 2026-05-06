package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import com.supererp.erp.enums.ProjectExpenseStatus;
import java.math.BigDecimal;

@Entity
@Table(name = "daily_labour_logs")
@Data @NoArgsConstructor @AllArgsConstructor @lombok.experimental.SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DailyLabourLog extends TenantAwareEntity {
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

