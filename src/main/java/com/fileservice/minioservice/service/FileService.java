package com.fileservice.minioservice.service;

import com.fileservice.minioservice.dto.FileDto;
import com.fileservice.minioservice.dto.FileUploadRequest;
import com.fileservice.minioservice.exception.FileServiceException;
import com.fileservice.minioservice.model.FileEntity;
import com.fileservice.minioservice.repository.FileRepository;
import io.minio.GetObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final MinioService minioService;
    private final FileRepository fileRepository;
    
    /**
     * Upload a file
     */
    @Transactional
    public FileDto uploadFile(FileUploadRequest request, String username) {
        try {
            FileEntity fileEntity = minioService.uploadFile(request, username);
            return minioService.convertToDto(fileEntity, 3600); // 1 hour URL expiry
        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            throw new FileServiceException.FileUploadException("Failed to upload file", e);
        }
    }
    
    /**
     * Get file by ID
     */
    @Transactional(readOnly = true)
    public FileDto getFileById(Long id, String username) {
        FileEntity fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new FileServiceException.FileNotFoundException("File not found with ID: " + id));
        
        // Check if user is the owner or has access (authorization will be implemented later)
        if (!fileEntity.getOwner().equals(username)) {
            // For now, just check ownership, OpenFGA will be integrated later
            throw new FileServiceException.FileAccessDeniedException("You don't have permission to access this file");
        }
        
        return minioService.convertToDto(fileEntity, 3600); // 1 hour URL expiry
    }
    
    /**
     * Get file content by ID
     */
    @Transactional(readOnly = true)
    public Optional<GetObjectResponse> getFileContent(Long id, String username) {
        FileEntity fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new FileServiceException.FileNotFoundException("File not found with ID: " + id));
        
        // Check if user is the owner or has access (authorization will be implemented later)
        if (!fileEntity.getOwner().equals(username)) {
            // For now, just check ownership, OpenFGA will be integrated later
            throw new FileServiceException.FileAccessDeniedException("You don't have permission to access this file");
        }
        
        return minioService.getFile(fileEntity.getObjectName());
    }
    
    /**
     * Get all files for a user
     */
    @Transactional(readOnly = true)
    public List<FileDto> getUserFiles(String username) {
        List<FileEntity> userFiles = fileRepository.findByOwner(username);
        return userFiles.stream()
                .map(file -> minioService.convertToDto(file, 3600)) // 1 hour URL expiry
                .collect(Collectors.toList());
    }
    
    /**
     * Delete file by ID
     */
    @Transactional
    public void deleteFile(Long id, String username) {
        FileEntity fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new FileServiceException.FileNotFoundException("File not found with ID: " + id));
        
        // Check if user is the owner or has delete permission (authorization will be implemented later)
        if (!fileEntity.getOwner().equals(username)) {
            // For now, just check ownership, OpenFGA will be integrated later
            throw new FileServiceException.FileAccessDeniedException("You don't have permission to delete this file");
        }
        
        try {
            // Delete from MinIO
            minioService.deleteFile(fileEntity.getObjectName());
            
            // Delete thumbnails if any
            for (String thumbnailPath : fileEntity.getThumbnailPaths()) {
                String thumbnailObjectName = thumbnailPath.substring(thumbnailPath.lastIndexOf("/") + 1);
                minioService.deleteFile(thumbnailObjectName);
            }
            
            // Delete from database
            fileRepository.delete(fileEntity);
        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage(), e);
            throw new FileServiceException.FileDeleteException("Failed to delete file", e);
        }
    }
    
    /**
     * Update file metadata
     */
    @Transactional
    public FileDto updateFileMetadata(Long id, String newFilename, Integer newExpiryDays, String username) {
        FileEntity fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new FileServiceException.FileNotFoundException("File not found with ID: " + id));
        
        // Check if user is the owner or has write permission (authorization will be implemented later)
        if (!fileEntity.getOwner().equals(username)) {
            // For now, just check ownership, OpenFGA will be integrated later
            throw new FileServiceException.FileAccessDeniedException("You don't have permission to update this file");
        }
        
        // Update filename if provided
        if (newFilename != null && !newFilename.isBlank()) {
            fileEntity.setFilename(newFilename);
        }
        
        // Update expiry date if provided
        if (newExpiryDays != null && newExpiryDays > 0) {
            fileEntity.setExpiresAt(LocalDateTime.now().plusDays(newExpiryDays));
        }
        
        // Save updated entity
        fileEntity = fileRepository.save(fileEntity);
        
        return minioService.convertToDto(fileEntity, 3600); // 1 hour URL expiry
    }
    
    /**
     * Search files by filename
     */
    @Transactional(readOnly = true)
    public List<FileDto> searchFilesByFilename(String filenamePattern, String username) {
        List<FileEntity> files = fileRepository.findByFilenameContainingIgnoreCase(filenamePattern);
        
        // Filter files by ownership or access permission (authorization will be implemented later)
        List<FileEntity> accessibleFiles = files.stream()
                .filter(file -> file.getOwner().equals(username))
                .collect(Collectors.toList());
        
        return accessibleFiles.stream()
                .map(file -> minioService.convertToDto(file, 3600)) // 1 hour URL expiry
                .collect(Collectors.toList());
    }
}
