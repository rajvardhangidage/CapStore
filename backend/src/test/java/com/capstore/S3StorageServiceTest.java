package com.capstore;

import com.capstore.exception.ApiException;
import com.capstore.service.ObjectStorageException;
import com.capstore.service.S3ObjectStorage;
import com.capstore.service.S3StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class S3StorageServiceTest {
    @Test
    void writesValidatedImageToConfiguredBucketAndReturnsCdnUrl() {
        RecordingObjectStorage s3 = new RecordingObjectStorage();
        S3StorageService storage = new S3StorageService(s3, "capstore-assets",
                "https://cdn.example.com/assets/", 1024);

        String url = storage.storeImage(image());

        assertTrue(url.startsWith("https://cdn.example.com/assets/uploads/"));
        assertEquals("capstore-assets", s3.bucket);
        assertEquals("image/webp", s3.contentType);
        assertTrue(s3.key.matches("uploads/[0-9a-f-]{36}\\.webp"));
    }

    @Test
    void reportsGatewayFailureWhenS3CannotStoreObject() {
        S3ObjectStorage s3 = (bucket, key, contentType, content, contentLength) -> {
            throw new ObjectStorageException(new IllegalStateException("unavailable"));
        };
        S3StorageService storage = new S3StorageService(s3, "capstore-assets",
                "https://cdn.example.com", 1024);

        ApiException error = org.junit.jupiter.api.Assertions.assertThrows(ApiException.class,
                () -> storage.storeImage(image()));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
    }

    private static final class RecordingObjectStorage implements S3ObjectStorage {
        private String bucket;
        private String key;
        private String contentType;

        @Override
        public void put(String bucket, String key, String contentType, InputStream content, long contentLength) {
            this.bucket = bucket;
            this.key = key;
            this.contentType = contentType;
        }
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("file", "logo.webp", "image/webp", new byte[]{1, 2, 3});
    }
}
