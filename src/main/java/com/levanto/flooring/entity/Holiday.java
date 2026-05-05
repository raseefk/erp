package com.levanto.flooring.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity @Table(name = "holidays")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Holiday {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @Column(nullable = false, length = 150)
    private String name;
}
