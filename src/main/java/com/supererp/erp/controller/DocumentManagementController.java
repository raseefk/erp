package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.entity.Document;
import com.supererp.erp.entity.DocumentFolder;
import com.supererp.erp.enums.DocumentAccessLevel;
import com.supererp.erp.enums.DocumentCategory;
import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.rbac.annotation.RequiresPermission;
import com.supererp.erp.rbac.Permissions;
import com.supererp.erp.service.DocumentManagementService;
import com.supererp.erp.service.DigitalSignatureService;
import com.supererp.erp.service.DocumentExpiryAlertService;
import com.supererp.erp.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.security.Principal;
import java.time.LocalDate;

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
    private final DigitalSignatureService signatureService;
    private final DocumentExpiryAlertService alertService;

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

    // ─────────────────────────────────────────────────────────────────────────
    // Digital Signatures — View Layer
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/digital-signatures")
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_VIEW)
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String digitalSignatures(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) String status,
                                    Model model) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        model.addAttribute("signaturePage",
            status != null && !status.isBlank()
                ? signatureService.getSignaturesByStatus(status, pageable)
                : signatureService.getAllSignatures(pageable));
        model.addAttribute("stats", signatureService.getSignatureStatistics());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("documents", dmsService.getAllDocumentsForLookup());
        model.addAttribute("activePage", "dms-signatures");
        return "dms/digital-signatures";
    }

    @GetMapping("/digital-signatures/{id}")
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_VIEW)
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String viewSignature(@PathVariable Long id, Model model) {
        var signature = signatureService.getSignatureById(id)
            .orElseThrow(() -> new IllegalArgumentException("Signature not found: " + id));
        model.addAttribute("signature", signature);
        model.addAttribute("documentSignatures",
            signatureService.getDocumentSignatures(signature.getDocument().getId()));
        model.addAttribute("activePage", "dms-signatures");
        return "dms/digital-signature-detail";
    }

    // AJAX — sign
    @PostMapping("/digital-signatures/{id}/sign")
    @ResponseBody
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_SIGN)
    public ResponseEntity<ApiResponse<?>> signDocument(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body,
            jakarta.servlet.http.HttpServletRequest req) {
        try {
            String ip = resolveIp(req);
            var signed = signatureService.signDocument(
                id, body.get("signatureData"), ip,
                req.getHeader("User-Agent"), body.get("geoLocation"));
            return ResponseEntity.ok(ApiResponse.ok("Document signed successfully.", signed.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // AJAX — verify
    @PostMapping("/digital-signatures/{id}/verify")
    @ResponseBody
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_VERIFY)
    public ResponseEntity<ApiResponse<?>> verifySignature(
            @PathVariable Long id, Principal principal) {
        try {
            String verifiedBy = principal != null ? principal.getName() : "system";
            signatureService.verifySignature(id, verifiedBy);
            return ResponseEntity.ok(ApiResponse.ok("Signature verified."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // AJAX — reject
    @PostMapping("/digital-signatures/{id}/reject")
    @ResponseBody
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_VERIFY)
    public ResponseEntity<ApiResponse<?>> rejectSignature(
            @PathVariable Long id, @RequestParam String reason) {
        try {
            signatureService.rejectSignature(id, reason);
            return ResponseEntity.ok(ApiResponse.ok("Signature rejected."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // AJAX — create request
    @PostMapping("/digital-signatures/request")
    @ResponseBody
    @RequiresPermission(Permissions.DMS_DIGITAL_SIGNATURE_SIGN)
    public ResponseEntity<ApiResponse<?>> createSignatureRequest(
            @RequestBody com.supererp.erp.entity.DigitalSignature sig) {
        try {
            Long docId = sig.getDocument().getId();
            var created = signatureService.createSignatureRequest(docId, sig);
            return ResponseEntity.ok(ApiResponse.ok("Signature request created.", created.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Expiry Alerts — View Layer
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/expiry-alerts")
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_VIEW)
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String expiryAlerts(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String status,
                               Model model) {
        var pageable = PageRequest.of(page, size);
        model.addAttribute("alertPage", alertService.getAllAlerts(pageable));
        model.addAttribute("stats", alertService.getAlertStatistics());
        model.addAttribute("upcoming30", alertService.getUpcomingExpiries(30));
        model.addAttribute("upcoming7", alertService.getUpcomingExpiries(7));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("documents", dmsService.getAllDocumentsForLookup());
        model.addAttribute("activePage", "dms-expiry-alerts");
        return "dms/expiry-alerts";
    }

    // AJAX — acknowledge
    @PostMapping("/expiry-alerts/{id}/acknowledge")
    @ResponseBody
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_MANAGE)
    public ResponseEntity<ApiResponse<?>> acknowledgeAlert(
            @PathVariable Long id, Principal principal) {
        try {
            String by = principal != null ? principal.getName() : "system";
            alertService.acknowledgeAlert(id, by);
            return ResponseEntity.ok(ApiResponse.ok("Alert acknowledged."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // AJAX — resolve
    @PostMapping("/expiry-alerts/{id}/resolve")
    @ResponseBody
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_MANAGE)
    public ResponseEntity<ApiResponse<?>> resolveAlert(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        try {
            Long renewalDocId = body.get("renewalDocumentId") != null
                ? Long.parseLong(body.get("renewalDocumentId")) : null;
            alertService.resolveAlert(id, body.get("resolutionNotes"), renewalDocId);
            return ResponseEntity.ok(ApiResponse.ok("Alert resolved."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // AJAX — delete
    @PostMapping("/expiry-alerts/{id}/delete")
    @ResponseBody
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_MANAGE)
    public ResponseEntity<ApiResponse<?>> deleteAlert(@PathVariable Long id) {
        try {
            alertService.deleteAlert(id);
            return ResponseEntity.ok(ApiResponse.ok("Alert deleted."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // AJAX — create alert
    @PostMapping("/expiry-alerts/save")
    @ResponseBody
    @RequiresPermission(Permissions.DMS_EXPIRY_ALERTS_MANAGE)
    public ResponseEntity<ApiResponse<?>> saveAlert(
            @RequestBody com.supererp.erp.entity.DocumentExpiryAlert alert) {
        try {
            Long docId = alert.getDocument().getId();
            var created = alertService.createAlert(docId, alert);
            return ResponseEntity.ok(ApiResponse.ok("Alert created.", created.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String resolveIp(jakarta.servlet.http.HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) ip = req.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) ip = req.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}
