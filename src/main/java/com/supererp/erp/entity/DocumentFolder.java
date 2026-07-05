package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Document folder in the Document Management System.
 * Supports hierarchical folder structure (parent-child).
 */
@Entity
@Table(name = "document_folders")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DocumentFolder extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    private DocumentFolder parentFolder;

    @OneToMany(mappedBy = "parentFolder", fetch = FetchType.LAZY)
    @Builder.Default
    private List<DocumentFolder> subFolders = new ArrayList<>();

    @Column(name = "icon_class", length = 50)
    @Builder.Default
    private String iconClass = "bi-folder";

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_by", length = 200)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
