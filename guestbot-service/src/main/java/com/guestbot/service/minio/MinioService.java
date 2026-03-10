package com.guestbot.service.minio;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-hotels}")
    private String hotelsBucket;

    @Value("${minio.bucket-rooms}")
    private String roomsBucket;

    public String uploadHotelPhoto(MultipartFile file, Long hotelId) {
        return upload(file, hotelsBucket, "hotels/" + hotelId);
    }

    public String uploadRoomPhoto(MultipartFile file, Long roomId) {
        return upload(file, roomsBucket, "rooms/" + roomId);
    }

    public void delete(String bucket, String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .build());
        } catch (Exception e) {
            log.error("Failed to delete object {}/{}: {}", bucket, key, e.getMessage());
            // Не бросаем исключение — удаление фото не критично
        }
    }

    public String getPresignedUrl(String bucket, String key) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(key)
                .expiry(7, TimeUnit.DAYS)
                .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    private String upload(MultipartFile file, String bucket, String prefix) {
        try {
            ensureBucketExists(bucket);

            String key = prefix + "/" + UUID.randomUUID() + getExtension(file);

            minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());

            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Created MinIO bucket: {}", bucket);
        }
    }

    private String getExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            return name.substring(name.lastIndexOf("."));
        }
        return ".jpg";
    }
}
