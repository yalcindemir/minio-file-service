package com.fileservice.minioservice.service;

import com.fileservice.minioservice.dto.ShareLinkDto;
import com.fileservice.minioservice.exception.FileServiceException;
import com.fileservice.minioservice.model.FileEntity;
import com.fileservice.minioservice.model.ShareLink;
import com.fileservice.minioservice.repository.FileRepository;
import com.fileservice.minioservice.repository.ShareLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareService {

    private final ShareLinkRepository shareLinkRepository;
    private final FileRepository fileRepository;
    private final MinioService minioService;
    private final AuthorizationService authorizationService;
    
    @Value("${file.default-expiry-days}")
    private int defaultExpiryDays;
    
    /**
     * Create a share link for a file
     */
    @Transactional
    public ShareLinkDto createShareLink(Long fileId, String permission, Integer expiryDays, String username) {
        // Verify file exists
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileServiceException.FileNotFoundException("File not found with ID: " + fileId));
        
        // Check if user has permission to share the file
        if (!fileEntity.getOwner().equals(username) && 
            !authorizationService.canReadFile(fileId.toString(), username)) {
            throw new FileServiceException.FileAccessDeniedException("You don't have permission to share this file");
        }
        
        // Validate permission
        if (!"READ".equalsIgnoreCase(permission) && !"WRITE".equalsIgnoreCase(permission)) {
            throw new IllegalArgumentException("Invalid permission. Must be READ or WRITE");
        }
        
        // Generate unique token
        String token = UUID.randomUUID().toString();
        
        // Calculate expiry date
        int days = expiryDays != null ? expiryDays : defaultExpiryDays;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(days);
        
        // Create and save share link
        ShareLink shareLink = ShareLink.builder()
                .token(token)
                .file(fileEntity)
                .permission(permission.toUpperCase())
                .expiresAt(expiresAt)
                .createdBy(username)
                .build();
        
        shareLink = shareLinkRepository.save(shareLink);
        
        // Generate presigned URL for the file
        String fileUrl = minioService.generatePresignedUrl(fileEntity.getObjectName(), days * 24 * 60 * 60);
        
        // Create DTO for response
        return ShareLinkDto.builder()
                .token(token)
                .fileId(fileId)
                .fileUrl(fileUrl)
                .expiresAt(expiresAt)
                .permission(permission)
                .build();
    }
    
    /**
     * Get share link by token
     */
    @Transactional(readOnly = true)
    public Optional<ShareLinkDto> getShareLinkByToken(String token) {
        return shareLinkRepository.findByToken(token)
                .map(this::convertToDto);
    }
    
    /**
     * Get all share links for a file
     */
    @Transactional(readOnly = true)
    public List<ShareLinkDto> getShareLinksForFile(Long fileId, String username) {
        // Verify file exists
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileServiceException.FileNotFoundException("File not found with ID: " + fileId));
        
        // Check if user has permission to view share links
        if (!fileEntity.getOwner().equals(username)) {
            throw new FileServiceException.FileAccessDeniedException("You don't have permission to view share links for this file");
        }
        
        return shareLinkRepository.findByFileId(fileId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all share links created by a user
     */
    @Transactional(readOnly = true)
    public List<ShareLinkDto> getShareLinksByUser(String username) {
        return shareLinkRepository.findByCreatedBy(username)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Delete a share link
     */
    @Transactional
    public void deleteShareLink(String token, String username) {
        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new FileServiceException.FileNotFoundException("Share link not found with token: " + token));
        
        // Check if user has permission to delete the share link
        if (!shareLink.getCreatedBy().equals(username)) {
            throw new FileServiceException.FileAccessDeniedException("You don't have permission to delete this share link");
        }
        
        shareLinkRepository.delete(shareLink);
    }
    
    /**
     * Validate a share link and get the associated file
     */
    @Transactional(readOnly = true)
    public FileEntity validateShareLink(String token, String requiredPermission) {
        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new FileServiceException.FileNotFoundException("Share link not found or expired"));
        
        // Check if the link has expired
        if (shareLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            shareLinkRepository.delete(shareLink);
            throw new FileServiceException.FileAccessDeniedException("Share link has expired");
        }
        
        // Check if the link has the required permission
        if (requiredPermission != null && 
            !shareLink.getPermission().equalsIgnoreCase(requiredPermission) && 
            !shareLink.getPermission().equalsIgnoreCase("WRITE")) { // WRITE permission includes READ
            throw new FileServiceException.FileAccessDeniedException("Share link does not have the required permission");
        }
        
        return shareLink.getFile();
    }
    
    /**
     * Convert ShareLink entity to ShareLinkDto
     */
    private ShareLinkDto convertToDto(ShareLink shareLink) {
        // Generate presigned URL for the file
        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now(), 
                shareLink.getExpiresAt());
        int expirySeconds = (int) (daysUntilExpiry * 24 * 60 * 60);
        
        String fileUrl = minioService.generatePresignedUrl(
                shareLink.getFile().getObjectName(), 
                expirySeconds > 0 ? expirySeconds : 3600); // Default to 1 hour if almost expired
        
        return ShareLinkDto.builder()
                .token(shareLink.getToken())
                .fileId(shareLink.getFile().getId())
                .fileUrl(fileUrl)
                .expiresAt(shareLink.getExpiresAt())
                .permission(shareLink.getPermission())
                .build();
    }
    
    /**
     * Clean up expired share links
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM every day
    @Transactional
    public void cleanupExpiredShareLinks() {
        LocalDateTime now = LocalDateTime.now();
        List<ShareLink> expiredLinks = shareLinkRepository.findExpiredLinks(now);
        
        for (ShareLink link : expiredLinks) {
            shareLinkRepository.delete(link);
            log.info("Deleted expired share link with token: {}", link.getToken());
        }
    }
}
