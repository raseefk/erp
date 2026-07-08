package com.supererp.erp.service;

import com.supererp.erp.tenant.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

/**
 * File storage service — single-tenant mode.
 * Files are stored under the upload directory without tenant prefixes.
 * Storage quota is tracked at application level.
 */
@Service
@Slf4j
public class FileStorageService {

    @Autowired
    private TenantService tenantService;

    private static final Set<String> ALLOWED_MIME = Set.of(
        "application/pdf", "image/jpeg", "image/jpg", "image/png", "image/webp");

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * Stores the file and returns its relative path.
     */
    public String store(MultipartFile file, String subfolder) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String mime = file.getContentType();
        if (!ALLOWED_MIME.contains(mime)) {
            throw new IllegalArgumentException(
                "Only PDF, JPG, PNG or WEBP files are allowed. Got: " + mime);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size must be under 5 MB.");
        }

        String ext      = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;
        Path   dir      = Paths.get(uploadDir, subfolder).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        Path dest = dir.resolve(filename);
        file.transferTo(dest.toFile());

        // Track upload size at application level
        tenantService.incrementUploadSize(file.getSize());

        log.info("Stored file: {}", dest);
        return subfolder + "/" + filename;
    }

    /**
     * Returns total upload size in GB for the application.
     */
    public double getUploadSizeInGB() {
        return tenantService.getUploadSizeInGB();
    }

    /**
     * Backward-compatible: returns upload size in GB (tenantId ignored).
     */
    public double getTenantUploadSizeInGB(UUID tenantId) {
        return getUploadSizeInGB();
    }

    /** Returns the absolute Path for serving a stored file. */
    public Path resolve(String relativePath) {
        Path root     = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new SecurityException("Path traversal detected");
        }
        return resolved;
    }

    /** Deletes a stored file and decrements tracked storage size. */
    public void delete(String relativePath) {
        if (relativePath == null) return;
        try {
            Path root     = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path resolved = root.resolve(relativePath).normalize();
            if (!resolved.startsWith(root)) {
                throw new SecurityException("Path traversal detected");
            }
            if (Files.exists(resolved)) {
                long size = Files.size(resolved);
                Files.deleteIfExists(resolved);
                tenantService.incrementUploadSize(-size);
            }
        } catch (IOException e) {
            log.warn("Could not delete file: {}", relativePath);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "bin";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "bin";
    }
}
