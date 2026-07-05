package com.supererp.erp.repository;

import com.supererp.erp.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    List<DocumentVersion> findByDocumentIdOrderByVersionDesc(Long documentId);

    Optional<DocumentVersion> findByDocumentIdAndVersion(Long documentId, Integer version);

    Optional<DocumentVersion> findFirstByDocumentIdOrderByVersionDesc(Long documentId);
}
