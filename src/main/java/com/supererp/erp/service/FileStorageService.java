package com.supererp.erp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;
import com.supererp.erp.tenant.TenantContext;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @org.springframework.beans.factory.annotation.Autowired
    private com.supererp.erp.tenant.TenantRepository tenantRepository;

    private static final Set<String> ALLOWED_MIME = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp");

    private static final long MAX_SIZE_BYTES = 2 * 1024 * 1024; // 2 MB

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * Saves the file to disk and returns the relative path.
     * 
     * @return relative path like "{tenantId}/expenses/abc123.pdf"
     */
    public String store(MultipartFile file, String subfolder) throws IOException {
        if (file == null || file.isEmpty())
            return null;

        String mime = file.getContentType();
        if (!ALLOWED_MIME.contains(mime)) {
            throw new IllegalArgumentException("Only PDF, JPG, PNG or WEBP files are allowed. Got: " + mime);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size must be under 2 MB.");
        }

        UUID tenantId = TenantContext.getTenantId();
        
        if (tenantId != null && tenantRepository != null) {
            com.supererp.erp.tenant.Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant != null) {
                double currentUsageGb = getTenantUploadSizeInGB(tenantId);
                double requestedSizeGb = file.getSize() / (1024.0 * 1024.0 * 1024.0);
                if ((currentUsageGb + requestedSizeGb) > tenant.getMaxStorageGb()) {
                    throw new IllegalArgumentException("File storage limit exceeded. Your organization is limited to " 
                        + tenant.getMaxStorageGb() + " GB.");
                }
            }
        }

        String tenantPrefix = tenantId != null ? tenantId.toString() + "/" : "";
        String namespacedSubfolder = tenantPrefix + subfolder;

        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;
        Path dir = Paths.get(uploadDir, namespacedSubfolder).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        Path dest = dir.resolve(filename);
        file.transferTo(dest.toFile());
        
        if (tenantId != null && tenantRepository != null) {
            com.supererp.erp.tenant.Tenant tenantUpdate = tenantRepository.findById(tenantId).orElse(null);
            if (tenantUpdate != null) {
                Long current = tenantUpdate.getUploadSizeBytes();
                tenantUpdate.setUploadSizeBytes((current == null ? 0L : current) + file.getSize());
                tenantRepository.save(tenantUpdate);
            }
        }
        
        log.info("Stored file: {}", dest);
        return namespacedSubfolder + "/" + filename;
    }

    /** Calculates the total size of uploads for a specific tenant in GB */
    public double getTenantUploadSizeInGB(UUID tenantId) {
        if (tenantId == null) return 0.0;
        com.supererp.erp.tenant.Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || tenant.getUploadSizeBytes() == null) return 0.0;
        return tenant.getUploadSizeBytes() / (1024.0 * 1024.0 * 1024.0);
    }

    /** Returns the absolute Path for serving a stored file */
    public Path resolve(String relativePath) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new SecurityException("Path traversal detected");
        }
        return resolved;
    }

    /** Deletes a stored file silently */
    public void delete(String relativePath) {
        if (relativePath == null)
            return;
        try {
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path resolved = root.resolve(relativePath).normalize();
            if (!resolved.startsWith(root)) {
                throw new SecurityException("Path traversal detected");
            }
            if (Files.exists(resolved)) {
                long size = Files.size(resolved);
                Files.deleteIfExists(resolved);
                
                UUID tenantId = TenantContext.getTenantId();
                if (tenantId != null && tenantRepository != null) {
                    com.supererp.erp.tenant.Tenant tenantUpdate = tenantRepository.findById(tenantId).orElse(null);
                    if (tenantUpdate != null) {
                        Long current = tenantUpdate.getUploadSizeBytes();
                        tenantUpdate.setUploadSizeBytes(Math.max(0, (current == null ? 0L : current) - size));
                        tenantRepository.save(tenantUpdate);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Could not delete file: {}", relativePath);
        }
    }

    private String getExtension(String filename) {
        if (filename == null)
            return "bin";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "bin";
    }
}
