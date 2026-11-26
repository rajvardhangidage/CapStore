package com.capstore.service;

import com.capstore.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "s3")
public class S3StorageService implements StorageService {
    private final S3ObjectStorage s3;
    private final String bucket;
    private final String publicBaseUrl;
    private final long maxFileSize;

    public S3StorageService(S3ObjectStorage s3,
                            @Value("${app.storage.s3.bucket}") String bucket,
                            @Value("${app.storage.s3.public-base-url}") String publicBaseUrl,
                            @Value("${app.storage.max-file-size-bytes:5242880}") long maxFileSize) {
        this.s3 = s3;
        this.bucket = bucket;
        this.publicBaseUrl = requireHttpBaseUrl(publicBaseUrl);
        if (maxFileSize <= 0) throw new IllegalArgumentException("UPLOAD_MAX_FILE_SIZE_BYTES must be positive");
        this.maxFileSize = maxFileSize;
        if (bucket == null || bucket.isBlank()) throw new IllegalArgumentException("S3_BUCKET is required for S3 storage");
    }

    @Override
    public String storeImage(MultipartFile file) {
        String extension = ImageUploadValidator.extension(file, maxFileSize);
        String key = "uploads/" + UUID.randomUUID() + "." + extension;
        try (var input = file.getInputStream()) {
            s3.put(bucket, key, file.getContentType(), input, file.getSize());
        } catch (IOException | ObjectStorageException e) {
            log.error("S3 image upload failed ({})", e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not store the uploaded image");
        }
        return publicBaseUrl + "/" + key;
    }

    private String requireHttpBaseUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getRawQuery() == null && uri.getRawFragment() == null
                    && uri.getRawUserInfo() == null)
                return value.replaceAll("/+$", "");
        } catch (IllegalArgumentException ignored) {
            // Report one configuration error for malformed and unsupported URLs.
        }
        throw new IllegalArgumentException("S3_PUBLIC_BASE_URL must be an HTTP(S) URL");
    }
}
