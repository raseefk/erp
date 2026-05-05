package com.levanto.flooring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "project_labours")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectLabour {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Column(precision = 10, scale = 2)
    private BigDecimal defaultWage = BigDecimal.ZERO;
}
