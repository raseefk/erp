package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.entity.Document;
import com.supererp.erp.entity.DocumentFolder;
import com.supererp.erp.enums.DocumentAccessLevel;
import com.supererp.erp.enums.DocumentCategory;
import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.service.DocumentManagementService;
import com.supererp.erp.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.security.Principal;

/**
 * Document Management System controller.
 * Vault, folder hierarchy, version control, access control.
 */
@Controller
@RequestMapping("/admin/dms")
@RequiredArgsConstructor
@RequiresFeature("DMS")
public class DocumentManagementController {

    private final DocumentManagementService dmsService;
    private final FileStorageService fileStorageService;

    // ─────────────────────────────────────────────────────────────────────────
    // Dashboard
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("metrics", dmsService.getDashboardMetrics());
        model.addAttribute("rootFolders", dmsService.getRootFolders());
        return "dms/dashboard";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Document List & Search
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/documents")
    public String listDocuments(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) String q,
                                 @RequestParam(required = false) DocumentCategory category,
                                 @RequestParam(required = false) Long folderId,
                                 Model model) {
        model.addAttribute("docPage", dmsService.searchDocuments(page, size, q, category, folderId));
        model.addAttribute("q", q);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedFolderId", folderId);
        model.addAttribute("categories", DocumentCategory.values());
        model.addAttribute("folders", dmsService.getAllFolders());
        model.addAttribute("currentPage", page);
        if (folderId != null) {
            model.addAttribute("currentFolder", dmsService.getFolderById(folderId));
        }
        return "dms/document-list";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Upload New Document
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/documents/upload")
    public String uploadForm(@RequestParam(required = false) Long folderId,
                              @RequestParam(required = false) String entityType,
                              @RequestParam(required = false) Long entityId,
                              Model model) {
        Document doc = new Document();
        if (folderId != null) {
            doc.setFolder(dmsService.getFolderById(folderId));
        }
        doc.setLinkedEntityType(entityType);
        doc.setLinkedEntityId(entityId);
        model.addAttribute("document", doc);
        model.addAttribute("categories", DocumentCategory.values());
        model.addAttribute("accessLevels", DocumentAccessLevel.values());
        model.addAttribute("folders", dmsService.getAllFolders());
        return "dms/document-upload";
    }

    @PostMapping("/documents/upload")
    public String upload(@ModelAttribute Document document,
                         @RequestParam("file") MultipartFile file,
                         Principal principal,
                         RedirectAttributes ra) {
        try {
            String uploader = principal != null ? principal.getName() : "system";
            dmsService.uploadDocument(document, file, uploader);
            ra.addFlashAttribute("success", "Document uploaded successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/admin/dms/documents";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // View & Edit Document
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/documents/{id}")
    public String viewDocument(@PathVariable Long id, Model model) {
        Document doc = dmsService.getDocumentById(id);
        model.addAttribute("document", doc);
        model.addAttribute("versions", dmsService.getVersionHistory(id));
        model.addAttribute("latestVersion", dmsService.getLatestVersion(id));
        return "dms/document-detail";
    }

    @GetMapping("/documents/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("document", dmsService.getDocumentById(id));
        model.addAttribute("categories", DocumentCategory.values());
        model.addAttribute("accessLevels", DocumentAccessLevel.values());
        model.addAttribute("folders", dmsService.getAllFolders());
        return "dms/document-edit";
    }

    @PostMapping("/documents/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute Document document,
                         RedirectAttributes ra) {
        try {
            document.setId(id);
            dmsService.updateDocument(document);
            ra.addFlashAttribute("success", "Document updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/dms/documents/" + id;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Version Upload
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/documents/{id}/new-version")
    public String uploadNewVersion(@PathVariable Long id,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestParam(required = false) String changeNotes,
                                    Principal principal,
                                    RedirectAttributes ra) {
        try {
            String uploader = principal != null ? principal.getName() : "system";
            dmsService.uploadNewVersion(id, file, changeNotes, uploader);
            ra.addFlashAttribute("success", "New version uploaded successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/dms/documents/" + id;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File Download
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                              @RequestParam(defaultValue = "0") int version) {
        try {
            var versionRecord = version > 0
                    ? dmsService.getVersion(id, version)
                    : dmsService.getLatestVersion(id);
            Path filePath = fileStorageService.resolve(versionRecord.getFilePath());
            Resource resource = new FileSystemResource(filePath.toFile());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            versionRecord.getMimeType() != null ? versionRecord.getMimeType() : "application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + versionRecord.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/documents/{id}/delete")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        try {
            dmsService.deleteDocument(id);
            return ResponseEntity.ok(ApiResponse.ok("Document deleted."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Folder Management
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/folders")
    public String folders(Model model) {
        model.addAttribute("rootFolders", dmsService.getRootFolders());
        model.addAttribute("allFolders", dmsService.getAllFolders());
        return "dms/folders";
    }

    @PostMapping("/folders/save")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> saveFolder(@RequestBody DocumentFolder folder) {
        try {
            DocumentFolder saved = dmsService.saveFolder(folder);
            return ResponseEntity.ok(ApiResponse.ok("Folder saved.", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/folders/{id}/delete")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> deleteFolder(@PathVariable Long id) {
        try {
            dmsService.deleteFolder(id);
            return ResponseEntity.ok(ApiResponse.ok("Folder deleted. Documents moved to root."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
