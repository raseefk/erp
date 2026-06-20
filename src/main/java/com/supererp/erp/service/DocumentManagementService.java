package com.supererp.erp.service;

import com.supererp.erp.entity.Document;
import com.supererp.erp.entity.DocumentFolder;
import com.supererp.erp.entity.DocumentVersion;
import com.supererp.erp.enums.DocumentAccessLevel;
import com.supererp.erp.enums.DocumentCategory;
import com.supererp.erp.rbac.annotation.AuditAction;
import com.supererp.erp.repository.DocumentFolderRepository;
import com.supererp.erp.repository.DocumentRepository;
import com.supererp.erp.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

/**
 * Document Management System service.
 * Handles documents, folder hierarchy, version control, expiry alerts, and access control.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentManagementService {

    private final DocumentRepository documentRepo;
    private final DocumentFolderRepository folderRepo;
    private final DocumentVersionRepository versionRepo;
    private final FileStorageService fileStorageService;

    // ─────────────────────────────────────────────────────────────────────────
    // Folder Management
    // ─────────────────────────────────────────────────────────────────────────

    public List<DocumentFolder> getRootFolders() {
        return folderRepo.findRootFolders();
    }

    public List<DocumentFolder> getAllFolders() {
        return folderRepo.findAllActive();
    }

    public DocumentFolder getFolderById(Long id) {
        return folderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + id));
    }

    @Transactional
    @AuditAction(value = "FOLDER_SAVE", entityType = "DocumentFolder")
    public DocumentFolder saveFolder(DocumentFolder folder) {
        return folderRepo.save(folder);
    }

    @Transactional
    public void deleteFolder(Long folderId) {
        // Move all documents to root before deleting
        DocumentFolder folder = getFolderById(folderId);
        List<Document> docs = documentRepo.searchDocuments(null, null, folderId,
                PageRequest.of(0, 1000)).getContent();
        docs.forEach(d -> d.setFolder(null));
        documentRepo.saveAll(docs);
        folder.setActive(false);
        folderRepo.save(folder);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Document CRUD
    // ─────────────────────────────────────────────────────────────────────────

    public Page<Document> searchDocuments(int page, int size, String q,
                                           DocumentCategory category, Long folderId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        return documentRepo.searchDocuments(q, category, folderId, pageable);
    }

    public Document getDocumentById(Long id) {
        return documentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
    }

    public List<Document> getLinkedDocuments(String entityType, Long entityId) {
        return documentRepo.findByLinkedEntity(entityType, entityId);
    }

    @Transactional
    @AuditAction(value = "DOCUMENT_UPLOAD", entityType = "Document")
    public Document uploadDocument(Document document, MultipartFile file, String uploadedBy) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required for upload.");
        }
        String filePath = fileStorageService.store(file, "documents");
        document.setCurrentVersion(1);
        document.setCreatedBy(uploadedBy);

        Document saved = documentRepo.save(document);

        DocumentVersion version = DocumentVersion.builder()
                .document(saved)
                .version(1)
                .filePath(filePath)
                .fileName(file.getOriginalFilename())
                .fileSizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .uploadedBy(uploadedBy)
                .build();
        versionRepo.save(version);
        return saved;
    }

    @Transactional
    @AuditAction(value = "DOCUMENT_UPDATE", entityType = "Document")
    public Document updateDocument(Document updated) {
        Document existing = getDocumentById(updated.getId());
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setCategory(updated.getCategory());
        existing.setFolder(updated.getFolder());
        existing.setTags(updated.getTags());
        existing.setAccessLevel(updated.getAccessLevel());
        existing.setAllowedDepartment(updated.getAllowedDepartment());
        existing.setAllowedRole(updated.getAllowedRole());
        existing.setExpiryDate(updated.getExpiryDate());
        existing.setExpiryAlertDays(updated.getExpiryAlertDays());
        existing.setLinkedEntityType(updated.getLinkedEntityType());
        existing.setLinkedEntityId(updated.getLinkedEntityId());
        return documentRepo.save(existing);
    }

    @Transactional
    @AuditAction(value = "DOCUMENT_NEW_VERSION", entityType = "Document")
    public Document uploadNewVersion(Long documentId, MultipartFile file,
                                      String changeNotes, String uploadedBy) throws Exception {
        Document doc = getDocumentById(documentId);
        String filePath = fileStorageService.store(file, "documents");

        int newVersion = doc.getCurrentVersion() + 1;
        doc.setCurrentVersion(newVersion);

        DocumentVersion version = DocumentVersion.builder()
                .document(doc)
                .version(newVersion)
                .filePath(filePath)
                .fileName(file.getOriginalFilename())
                .fileSizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .changeNotes(changeNotes)
                .uploadedBy(uploadedBy)
                .build();
        versionRepo.save(version);
        return documentRepo.save(doc);
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        Document doc = getDocumentById(documentId);
        doc.setActive(false);
        documentRepo.save(doc);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Version Management
    // ─────────────────────────────────────────────────────────────────────────

    public List<DocumentVersion> getVersionHistory(Long documentId) {
        return versionRepo.findByDocumentIdOrderByVersionDesc(documentId);
    }

    public DocumentVersion getVersion(Long documentId, Integer version) {
        return versionRepo.findByDocumentIdAndVersion(documentId, version)
                .orElseThrow(() -> new IllegalArgumentException("Version not found"));
    }

    public DocumentVersion getLatestVersion(Long documentId) {
        return versionRepo.findFirstByDocumentIdOrderByVersionDesc(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No version found for document: " + documentId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Access Control Check
    // ─────────────────────────────────────────────────────────────────────────

    public boolean canAccess(Document document, String userDepartment, String userRole) {
        DocumentAccessLevel level = document.getAccessLevel();
        return switch (level) {
            case PUBLIC -> true;
            case DEPARTMENT -> document.getAllowedDepartment() != null
                    && document.getAllowedDepartment().equalsIgnoreCase(userDepartment);
            case ROLE -> document.getAllowedRole() != null
                    && document.getAllowedRole().equalsIgnoreCase(userRole);
            case PRIVATE -> false; // Only owner — handled by controller
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Expiry Alerts — runs daily at 8 AM
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 8 * * *")
    public void sendExpiryAlerts() {
        LocalDate alertThreshold = LocalDate.now().plusDays(30);
        List<Document> docs = documentRepo.findDocumentsNeedingExpiryAlert(alertThreshold);
        docs.forEach(doc -> {
            log.warn("EXPIRY ALERT: Document '{}' (ID={}) expires on {}",
                    doc.getTitle(), doc.getId(), doc.getExpiryDate());
            doc.setAlertSent(true);
        });
        if (!docs.isEmpty()) documentRepo.saveAll(docs);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dashboard Metrics
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalDocuments", documentRepo.countByActiveTrue());
        metrics.put("expiringIn30Days", documentRepo.countExpiringSoon(LocalDate.now().plusDays(30)));
        metrics.put("totalFolders", folderRepo.findAllActive().size());
        List<Document> expiring = documentRepo.findExpiringBetween(LocalDate.now(), LocalDate.now().plusDays(30));
        metrics.put("expiringDocuments", expiring);
        return metrics;
    }
}
