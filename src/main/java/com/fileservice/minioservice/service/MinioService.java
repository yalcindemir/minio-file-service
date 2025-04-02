package com.fileservice.minioservice.service;

import com.fileservice.minioservice.dto.FileDto;
import com.fileservice.minioservice.dto.FileUploadRequest;
import com.fileservice.minioservice.dto.ImageDimension;
import com.fileservice.minioservice.exception.FileServiceException;
import com.fileservice.minioservice.model.FileEntity;
import com.fileservice.minioservice.repository.FileRepository;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;
    private final FileRepository fileRepository;
    private final ImageService imageService;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${file.default-expiry-days}")
    private int defaultExpiryDays;

    /**
     * Initialize MinIO bucket if it doesn't exist
     */
    public void initialize() {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Error initializing MinIO bucket: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize MinIO bucket", e);
        }
    }

    /**
     * Upload a file to MinIO
     */
    public FileEntity uploadFile(FileUploadRequest request, String username) throws IOException {
        MultipartFile file = request.getFile();
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String filename = request.getCustomFilename() != null ? request.getCustomFilename() : originalFilename;
        
        // Generate a unique object name
        String objectName = UUID.randomUUID().toString() + "_" + filename;
        String path = "/" + bucketName + "/" + objectName;
        
        // Calculate expiry date
        int expiryDays = request.getExpiryDays() != null ? request.getExpiryDays() : defaultExpiryDays;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(expiryDays);
        
        try {
            // Upload file to MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .contentType(contentType)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build()
            );
            
            // Create file entity
            FileEntity fileEntity = FileEntity.builder()
                .filename(filename)
                .contentType(contentType)
                .path(path)
                .size(file.getSize())
                .bucketName(bucketName)
                .objectName(objectName)
                .owner(username)
                .expiresAt(expiresAt)
                .build();
            
            // Generate thumbnails if it's an image
            if (imageService.isImage(contentType) && request.getThumbnailDimensions() != null && !request.getThumbnailDimensions().isEmpty()) {
                Set<String> thumbnailPaths = imageService.generateThumbnails(file, request.getThumbnailDimensions(), objectName);
                fileEntity.setThumbnailPaths(thumbnailPaths);
            }
            
            return fileRepository.save(fileEntity);
        } catch (Exception e) {
            log.error("Error uploading file to MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    /**
     * Get a file from MinIO
     */
    public Optional<GetObjectResponse> getFile(String objectName) {
        try {
            return Optional.of(minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            ));
        } catch (Exception e) {
            log.error("Error getting file from MinIO: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Delete a file from MinIO
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error deleting file from MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    /**
     * Generate a presigned URL for file download
     */
    public String generatePresignedUrl(String objectName, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(expirySeconds, TimeUnit.SECONDS)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Convert FileEntity to FileDto with download URL
     */
    public FileDto convertToDto(FileEntity fileEntity, int urlExpirySeconds) {
        String downloadUrl = generatePresignedUrl(fileEntity.getObjectName(), urlExpirySeconds);
        
        return FileDto.builder()
            .id(fileEntity.getId())
            .filename(fileEntity.getFilename())
            .contentType(fileEntity.getContentType())
            .path(fileEntity.getPath())
            .size(fileEntity.getSize())
            .owner(fileEntity.getOwner())
            .createdAt(fileEntity.getCreatedAt())
            .updatedAt(fileEntity.getUpdatedAt())
            .expiresAt(fileEntity.getExpiresAt())
            .thumbnailPaths(fileEntity.getThumbnailPaths())
            .downloadUrl(downloadUrl)
            .build();
    }

    /**
     * Clean up expired files
     */
    public void cleanupExpiredFiles() {
        LocalDateTime now = LocalDateTime.now();
        List<FileEntity> expiredFiles = fileRepository.findExpiredFiles(now);
        
        for (FileEntity file : expiredFiles) {
            try {
                // Delete from MinIO
                deleteFile(file.getObjectName());
                
                // Delete thumbnails if any
                for (String thumbnailPath : file.getThumbnailPaths()) {
                    String thumbnailObjectName = thumbnailPath.substring(thumbnailPath.lastIndexOf("/") + 1);
                    deleteFile(thumbnailObjectName);
                }
                
                // Delete from database
                fileRepository.delete(file);
                
                log.info("Deleted expired file: {}", file.getFilename());
            } catch (Exception e) {
                log.error("Error deleting expired file {}: {}", file.getFilename(), e.getMessage(), e);
            }
        }
    }
}
