package com.fileservice.minioservice.service;

import com.fileservice.minioservice.dto.ImageDimension;
import com.fileservice.minioservice.exception.FileServiceException;
import com.fileservice.minioservice.model.FileEntity;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final MinioClient minioClient;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    /**
     * Check if file is an image
     */
    public boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
    
    /**
     * Generate thumbnails for an image
     */
    public Set<String> generateThumbnails(MultipartFile file, List<ImageDimension> dimensions, String originalObjectName) {
        if (file == null || !isImage(file.getContentType()) || dimensions == null || dimensions.isEmpty()) {
            return new HashSet<>();
        }
        
        Set<String> thumbnailPaths = new HashSet<>();
        
        try {
            for (ImageDimension dimension : dimensions) {
                String thumbnailPath = createThumbnail(file, dimension, originalObjectName);
                if (thumbnailPath != null) {
                    thumbnailPaths.add(thumbnailPath);
                }
            }
        } catch (Exception e) {
            log.error("Error generating thumbnails: {}", e.getMessage(), e);
            // Continue without thumbnails if there's an error
        }
        
        return thumbnailPaths;
    }
    
    /**
     * Create a single thumbnail with specified dimensions
     */
    private String createThumbnail(MultipartFile file, ImageDimension dimension, String originalObjectName) throws IOException {
        if (dimension.getWidth() <= 0 || dimension.getHeight() <= 0) {
            return null;
        }
        
        // Generate thumbnail object name
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String thumbnailObjectName = generateThumbnailObjectName(originalObjectName, dimension, fileExtension);
        String thumbnailPath = "/" + bucketName + "/" + thumbnailObjectName;
        
        // Create thumbnail
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(file.getInputStream())
                .size(dimension.getWidth(), dimension.getHeight())
                .keepAspectRatio(true)
                .toOutputStream(outputStream);
        
        // Upload thumbnail to MinIO
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(thumbnailObjectName)
                    .contentType(file.getContentType())
                    .stream(inputStream, outputStream.size(), -1)
                    .build()
            );
            
            return thumbnailPath;
        } catch (Exception e) {
            log.error("Error uploading thumbnail to MinIO: {}", e.getMessage(), e);
            throw new FileServiceException.FileUploadException("Failed to upload thumbnail", e);
        }
    }
    
    /**
     * Generate a unique name for the thumbnail
     */
    private String generateThumbnailObjectName(String originalObjectName, ImageDimension dimension, String fileExtension) {
        String baseName = originalObjectName.substring(0, originalObjectName.lastIndexOf('.'));
        return baseName + "_" + dimension.getWidth() + "x" + dimension.getHeight() + fileExtension;
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return ".jpg"; // Default extension
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
