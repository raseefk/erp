package com.supererp.erp.rbac.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "menus")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Menu {

    @Id
    @Column(length = 60)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Feature feature;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "url_pattern")
    private String urlPattern;

    private String icon;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;

    @OneToMany(mappedBy = "menu", fetch = FetchType.LAZY)
    private Set<Permission> permissions;
}
