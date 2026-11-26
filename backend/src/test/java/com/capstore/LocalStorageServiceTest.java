package com.capstore;

import com.capstore.exception.ApiException;
import com.capstore.service.LocalStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void storesValidatedImageAndReturnsLocalUrl() throws Exception {
        LocalStorageService storage = new LocalStorageService(tempDir.toString(), 1024);
        MockMultipartFile file = image("photo.JPG", "image/jpeg", new byte[]{1, 2, 3});

        String url = storage.storeImage(file);

        assertTrue(url.matches("/uploads/[0-9a-f-]{36}\\.jpg"));
        assertTrue(Files.exists(tempDir.resolve(url.substring("/uploads/".length()))));
    }

    @Test
    void rejectsMimeTypeThatDoesNotMatchTheImageExtension() throws Exception {
        LocalStorageService storage = new LocalStorageService(tempDir.toString(), 1024);

        ApiException error = assertThrows(ApiException.class,
                () -> storage.storeImage(image("image.jpg", "image/svg+xml", new byte[]{1})));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    @Test
    void rejectsImagesLargerThanConfiguredLimit() throws Exception {
        LocalStorageService storage = new LocalStorageService(tempDir.toString(), 2);

        ApiException error = assertThrows(ApiException.class,
                () -> storage.storeImage(image("image.png", "image/png", new byte[]{1, 2, 3})));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    private MockMultipartFile image(String name, String contentType, byte[] data) {
        return new MockMultipartFile("file", name, contentType, data);
    }
}
