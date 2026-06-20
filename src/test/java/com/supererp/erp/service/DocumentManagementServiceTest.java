package com.supererp.erp.service;

import com.supererp.erp.entity.*;
import com.supererp.erp.enums.*;
import com.supererp.erp.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentManagementService.
 */
@ExtendWith(MockitoExtension.class)
class DocumentManagementServiceTest {

    @Mock DocumentRepository documentRepo;
    @Mock DocumentFolderRepository folderRepo;
    @Mock DocumentVersionRepository versionRepo;
    @Mock FileStorageService fileStorageService;

    @InjectMocks DocumentManagementService dmsService;

    // ─── Upload Tests ────────────────────────────────────────────────────────

    @Test
    void uploadDocument_savesDocumentAndCreatesVersion() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf",
                "application/pdf", "PDF content".getBytes());

        Document doc = buildDocument();
        when(fileStorageService.store(any(), any())).thenReturn("tenantId/documents/abc.pdf");
        when(documentRepo.save(any())).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });
        when(versionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Document result = dmsService.uploadDocument(doc, file, "user1");

        assertThat(result.getCurrentVersion()).isEqualTo(1);
        assertThat(result.getCreatedBy()).isEqualTo("user1");
        verify(versionRepo).save(argThat(v -> v.getVersion() == 1 && v.getFilePath().equals("tenantId/documents/abc.pdf")));
    }

    @Test
    void uploadDocument_throwsWhenFileNull() {
        Document doc = buildDocument();

        assertThatThrownBy(() -> dmsService.uploadDocument(doc, null, "user1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is required");
    }

    @Test
    void uploadNewVersion_incrementsVersionAndCreatesRecord() throws Exception {
        Document existing = buildDocument();
        existing.setId(1L);
        existing.setCurrentVersion(2);

        MockMultipartFile file = new MockMultipartFile("file", "v3.pdf",
                "application/pdf", "PDF v3".getBytes());

        when(documentRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(fileStorageService.store(any(), any())).thenReturn("path/to/v3.pdf");
        when(documentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Document result = dmsService.uploadNewVersion(1L, file, "Updated terms", "user2");

        assertThat(result.getCurrentVersion()).isEqualTo(3);
        verify(versionRepo).save(argThat(v -> v.getVersion() == 3 && "Updated terms".equals(v.getChangeNotes())));
    }

    // ─── Folder Tests ─────────────────────────────────────────────────────────

    @Test
    void saveFolder_persistsFolder() {
        DocumentFolder folder = new DocumentFolder();
        folder.setName("HR Documents");

        when(folderRepo.save(any())).thenAnswer(inv -> {
            DocumentFolder f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });

        DocumentFolder result = dmsService.saveFolder(folder);

        assertThat(result.getId()).isEqualTo(1L);
        verify(folderRepo).save(folder);
    }

    @Test
    void getRootFolders_returnsOnlyRootLevel() {
        DocumentFolder root = buildFolder(null);
        when(folderRepo.findRootFolders()).thenReturn(List.of(root));

        List<DocumentFolder> result = dmsService.getRootFolders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParentFolder()).isNull();
    }

    // ─── Delete Document Tests ────────────────────────────────────────────────

    @Test
    void deleteDocument_marksAsInactive() {
        Document doc = buildDocument();
        doc.setId(1L);
        doc.setActive(true);

        when(documentRepo.findById(1L)).thenReturn(Optional.of(doc));
        when(documentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dmsService.deleteDocument(1L);

        assertThat(doc.isActive()).isFalse();
    }

    // ─── Access Control Tests ─────────────────────────────────────────────────

    @Test
    void canAccess_publicDocumentAlwaysAccessible() {
        Document doc = buildDocument();
        doc.setAccessLevel(DocumentAccessLevel.PUBLIC);

        assertThat(dmsService.canAccess(doc, "Finance", "MANAGER")).isTrue();
        assertThat(dmsService.canAccess(doc, null, null)).isTrue();
    }

    @Test
    void canAccess_departmentDocumentMatchesDepartment() {
        Document doc = buildDocument();
        doc.setAccessLevel(DocumentAccessLevel.DEPARTMENT);
        doc.setAllowedDepartment("Finance");

        assertThat(dmsService.canAccess(doc, "Finance", "MANAGER")).isTrue();
        assertThat(dmsService.canAccess(doc, "HR", "MANAGER")).isFalse();
    }

    @Test
    void canAccess_roleDocumentMatchesRole() {
        Document doc = buildDocument();
        doc.setAccessLevel(DocumentAccessLevel.ROLE);
        doc.setAllowedRole("ADMIN");

        assertThat(dmsService.canAccess(doc, "Finance", "ADMIN")).isTrue();
        assertThat(dmsService.canAccess(doc, "Finance", "VIEWER")).isFalse();
    }

    @Test
    void canAccess_privateDocumentAlwaysDenied() {
        Document doc = buildDocument();
        doc.setAccessLevel(DocumentAccessLevel.PRIVATE);

        assertThat(dmsService.canAccess(doc, "Finance", "ADMIN")).isFalse();
    }

    // ─── Version History Tests ────────────────────────────────────────────────

    @Test
    void getVersionHistory_returnsVersionsInDescendingOrder() {
        DocumentVersion v1 = buildVersion(1L, 1);
        DocumentVersion v2 = buildVersion(1L, 2);

        when(versionRepo.findByDocumentIdOrderByVersionDesc(1L)).thenReturn(List.of(v2, v1));

        List<DocumentVersion> result = dmsService.getVersionHistory(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersion()).isEqualTo(2);
    }

    @Test
    void getLatestVersion_returnsHighestVersion() {
        DocumentVersion latest = buildVersion(1L, 3);
        when(versionRepo.findFirstByDocumentIdOrderByVersionDesc(1L)).thenReturn(Optional.of(latest));

        DocumentVersion result = dmsService.getLatestVersion(1L);

        assertThat(result.getVersion()).isEqualTo(3);
    }

    @Test
    void getLatestVersion_throwsWhenNoVersionFound() {
        when(versionRepo.findFirstByDocumentIdOrderByVersionDesc(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dmsService.getLatestVersion(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No version found");
    }

    // ─── Dashboard Metrics Tests ──────────────────────────────────────────────

    @Test
    void getDashboardMetrics_returnsAllMetrics() {
        when(documentRepo.countByActiveTrue()).thenReturn(50L);
        when(documentRepo.countExpiringSoon(any())).thenReturn(3L);
        when(folderRepo.findAllActive()).thenReturn(List.of(buildFolder(null)));
        when(documentRepo.findExpiringBetween(any(), any())).thenReturn(List.of());

        Map<String, Object> metrics = dmsService.getDashboardMetrics();

        assertThat(metrics).containsKey("totalDocuments");
        assertThat(metrics.get("totalDocuments")).isEqualTo(50L);
        assertThat(metrics.get("expiringIn30Days")).isEqualTo(3L);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Document buildDocument() {
        Document doc = new Document();
        doc.setTitle("Test Contract");
        doc.setCategory(DocumentCategory.CONTRACT);
        doc.setAccessLevel(DocumentAccessLevel.PUBLIC);
        doc.setCurrentVersion(1);
        doc.setActive(true);
        return doc;
    }

    private DocumentFolder buildFolder(DocumentFolder parent) {
        DocumentFolder folder = new DocumentFolder();
        folder.setId(1L);
        folder.setName("Test Folder");
        folder.setActive(true);
        folder.setParentFolder(parent);
        return folder;
    }

    private DocumentVersion buildVersion(Long docId, int version) {
        DocumentVersion v = new DocumentVersion();
        v.setId((long) version);
        v.setVersion(version);
        v.setFilePath("path/v" + version + ".pdf");
        v.setFileName("doc_v" + version + ".pdf");
        return v;
    }
}
