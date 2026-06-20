package com.supererp.erp.repository;

import com.supererp.erp.entity.Document;
import com.supererp.erp.enums.DocumentCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    @EntityGraph(attributePaths = {"folder", "versions"})
    Optional<Document> findById(Long id);

    @Query("SELECT d FROM Document d LEFT JOIN d.folder f WHERE d.active = true AND " +
           "(:q IS NULL OR :q = '' OR LOWER(d.title) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(d.tags) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "AND (:category IS NULL OR d.category = :category) " +
           "AND (:folderId IS NULL OR d.folder.id = :folderId) " +
           "ORDER BY d.updatedAt DESC")
    Page<Document> searchDocuments(@Param("q") String q,
                                   @Param("category") DocumentCategory category,
                                   @Param("folderId") Long folderId,
                                   Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.active = true AND d.expiryDate IS NOT NULL " +
           "AND d.expiryDate <= :alertDate AND d.alertSent = false")
    List<Document> findDocumentsNeedingExpiryAlert(@Param("alertDate") LocalDate alertDate);

    @Query("SELECT d FROM Document d WHERE d.active = true AND d.expiryDate IS NOT NULL " +
           "AND d.expiryDate BETWEEN :from AND :to ORDER BY d.expiryDate ASC")
    List<Document> findExpiringBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT d FROM Document d WHERE d.active = true AND d.linkedEntityType = :entityType " +
           "AND d.linkedEntityId = :entityId ORDER BY d.updatedAt DESC")
    List<Document> findByLinkedEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    long countByActiveTrue();

    @Query("SELECT COUNT(d) FROM Document d WHERE d.active = true AND d.expiryDate IS NOT NULL " +
           "AND d.expiryDate <= :date")
    long countExpiringSoon(@Param("date") LocalDate date);
}
