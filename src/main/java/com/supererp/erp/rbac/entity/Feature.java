package com.supererp.erp.rbac.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "features")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Feature {

    @Id
    @Column(length = 60)
    private String id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    private String description;
    private String icon;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;

    @OneToMany(mappedBy = "feature", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private Set<Menu> menus;
}
