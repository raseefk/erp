package com.supererp.erp.rbac.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Permission {

    @Id
    @Column(length = 100)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Feature feature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Menu menu;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    private String description;

    @Column(nullable = false, length = 50)
    private String action; // VIEW, CREATE, EDIT, DELETE, APPROVE, EXPORT_PDF, etc.
}
