package com.capstore.controller;

import com.capstore.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** Image upload for logged-in users (product images for admins, logos for B2B quote requests). */
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {
    private final StorageService storage;

    @PostMapping
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        return Map.of("url", storage.storeImage(file));
    }
}
