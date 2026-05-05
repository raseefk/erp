package com.levanto.flooring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private static final Set<String> ALLOWED_MIME = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp");

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * Saves the file to disk and returns the relative path.
     * 
     * @return relative path like "expenses/abc123.pdf"
     */
    public String store(MultipartFile file, String subfolder) throws IOException {
        if (file == null || file.isEmpty())
            return null;

        String mime = file.getContentType();
        if (!ALLOWED_MIME.contains(mime)) {
            throw new IllegalArgumentException("Only PDF, JPG, PNG or WEBP files are allowed. Got: " + mime);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size must be under 10 MB.");
        }

        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;
        Path dir = Paths.get(uploadDir, subfolder).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        Path dest = dir.resolve(filename);
        file.transferTo(dest.toFile());
        log.info("Stored file: {}", dest);
        return subfolder + "/" + filename;
    }

    /** Returns the absolute Path for serving a stored file */
    public Path resolve(String relativePath) {
        return Paths.get(uploadDir).resolve(relativePath);
    }

    /** Deletes a stored file silently */
    public void delete(String relativePath) {
        if (relativePath == null)
            return;
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(relativePath));
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
