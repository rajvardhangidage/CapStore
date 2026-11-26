package com.capstore.service;

import com.capstore.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {
    private final Path root;
    private final long maxFileSize;

    public LocalStorageService(@Value("${app.upload.dir}") String dir,
                               @Value("${app.storage.max-file-size-bytes:5242880}") long maxFileSize)
            throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        if (maxFileSize <= 0) throw new IllegalArgumentException("UPLOAD_MAX_FILE_SIZE_BYTES must be positive");
        this.maxFileSize = maxFileSize;
        Files.createDirectories(root);
    }

    public Path root() {
        return root;
    }

    @Override
    public String storeImage(MultipartFile file) {
        String extension = ImageUploadValidator.extension(file, maxFileSize);
        String name = UUID.randomUUID() + "." + extension;
        try {
            file.transferTo(root.resolve(name));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the uploaded image");
        }
        return "/uploads/" + name;
    }
}
