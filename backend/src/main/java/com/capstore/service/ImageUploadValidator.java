package com.capstore.service;

import com.capstore.exception.ApiException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;

final class ImageUploadValidator {
    private static final Map<String, String> MIME_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif");

    private ImageUploadValidator() {}

    static String extension(MultipartFile file, long maxFileSize) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("No file uploaded");
        if (file.getSize() > maxFileSize) throw ApiException.badRequest("Image exceeds the maximum upload size");

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String extension = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
        String expectedContentType = MIME_TYPES.get(extension);
        if (expectedContentType == null || !expectedContentType.equalsIgnoreCase(file.getContentType()))
            throw ApiException.badRequest("Only JPG, PNG, WEBP or GIF images are allowed");
        return extension;
    }
}
