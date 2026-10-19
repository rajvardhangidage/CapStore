package com.capstore.service;

import com.capstore.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded images on local disk (served from /uploads/**).
 * To move to S3/Cloudinary later, only this class needs to change.
 */
@Service
public class StorageService {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private final Path root;

    public StorageService(@Value("${app.upload.dir}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public Path root() { return root; }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("No file uploaded");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
        String ct = file.getContentType();
        if (!ALLOWED.contains(ext) || ct == null || !ct.startsWith("image/"))
            throw ApiException.badRequest("Only JPG, PNG, WEBP or GIF images are allowed");
        String name = UUID.randomUUID() + "." + ext;
        try {
            file.transferTo(root.resolve(name));
        } catch (IOException e) {
            throw ApiException.badRequest("Could not save the file");
        }
        return "/uploads/" + name;
    }
}
