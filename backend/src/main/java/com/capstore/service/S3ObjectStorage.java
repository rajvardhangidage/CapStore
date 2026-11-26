package com.capstore.service;

import java.io.InputStream;

public interface S3ObjectStorage {
    void put(String bucket, String key, String contentType, InputStream content, long contentLength);
}
