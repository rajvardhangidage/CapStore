package com.capstore.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String storeImage(MultipartFile file);
}
