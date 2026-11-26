package com.capstore.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Component
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "s3")
public class AwsS3ObjectStorage implements S3ObjectStorage {
    private final S3Client s3;

    public AwsS3ObjectStorage(S3Client s3) {
        this.s3 = s3;
    }

    @Override
    public void put(String bucket, String key, String contentType, InputStream content, long contentLength) {
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(content, contentLength));
        } catch (SdkException e) {
            throw new ObjectStorageException(e);
        }
    }
}
