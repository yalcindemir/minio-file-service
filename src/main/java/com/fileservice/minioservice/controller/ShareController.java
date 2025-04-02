package com.fileservice.minioservice.controller;

import com.fileservice.minioservice.dto.FileDto;
import com.fileservice.minioservice.dto.ShareLinkDto;
import com.fileservice.minioservice.exception.FileServiceException;
import com.fileservice.minioservice.model.FileEntity;
import com.fileservice.minioservice.service.FileService;
import com.fileservice.minioservice.service.MinioService;
import com.fileservice.minioservice.service.ShareService;
import io.minio.GetObjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/shares")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Sharing", description = "API endpoints for file sharing operations")
public class ShareController {

    private final ShareService shareService;
    private final FileService fileService;
    private final MinioService minioService;

    @PostMapping
    @Operation(
        summary = "Create a share link",
        description = "Create a temporary share link for a file with specified permissions"
    )
    @ApiResponse(responseCode = "201", description = "Share link created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "File not found")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ShareLinkDto> createShareLink(
            @RequestParam Long fileId,
            @RequestParam String permission,
            @RequestParam(required = false) Integer expiryDays,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt.getSubject();
        ShareLinkDto shareLink = shareService.createShareLink(fileId, permission, expiryDays, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(shareLink);
    }

    @GetMapping
    @Operation(
        summary = "Get all share links created by user",
        description = "Retrieve all share links created by the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Share links retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ShareLinkDto>> getUserShareLinks(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();
        List<ShareLinkDto> shareLinks = shareService.getShareLinksByUser(username);
        return ResponseEntity.ok(shareLinks);
    }

    @GetMapping("/files/{fileId}")
    @Operation(
        summary = "Get all share links for a file",
        description = "Retrieve all share links for a specific file"
    )
    @ApiResponse(responseCode = "200", description = "Share links retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "File not found")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ShareLinkDto>> getFileShareLinks(
            @PathVariable Long fileId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt.getSubject();
        List<ShareLinkDto> shareLinks = shareService.getShareLinksForFile(fileId, username);
        return ResponseEntity.ok(shareLinks);
    }

    @DeleteMapping("/{token}")
    @Operation(
        summary = "Delete a share link",
        description = "Delete a specific share link by its token"
    )
    @ApiResponse(responseCode = "204", description = "Share link deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Share link not found")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteShareLink(
            @PathVariable String token,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt.getSubject();
        shareService.deleteShareLink(token, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/access/{token}")
    @Operation(
        summary = "Access a shared file by token",
        description = "Access file metadata using a share link token (no authentication required)"
    )
    @ApiResponse(responseCode = "200", description = "File metadata retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Share link not found or expired")
    public ResponseEntity<FileDto> accessSharedFile(@PathVariable String token) {
        // Validate share link and get file (requires READ permission)
        FileEntity fileEntity = shareService.validateShareLink(token, "READ");
        
        // Convert to DTO with presigned URL
        FileDto fileDto = minioService.convertToDto(fileEntity, 3600); // 1 hour URL expiry
        
        return ResponseEntity.ok(fileDto);
    }

    @GetMapping("/access/{token}/content")
    @Operation(
        summary = "Download a shared file by token",
        description = "Download file content using a share link token (no authentication required)"
    )
    @ApiResponse(responseCode = "200", description = "File content retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Share link not found or expired")
    public ResponseEntity<InputStreamResource> downloadSharedFile(@PathVariable String token) {
        // Validate share link and get file (requires READ permission)
        FileEntity fileEntity = shareService.validateShareLink(token, "READ");
        
        // Get file content
        Optional<GetObjectResponse> fileContent = minioService.getFile(fileEntity.getObjectName());
        
        if (fileContent.isPresent()) {
            GetObjectResponse response = fileContent.get();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(fileEntity.getContentType()));
            headers.setContentDispositionFormData("attachment", fileEntity.getFilename());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(response));
        } else {
            throw new FileServiceException.FileNotFoundException("File content not found");
        }
    }

    @GetMapping("/validate/{token}")
    @Operation(
        summary = "Validate a share link",
        description = "Check if a share link is valid and has not expired"
    )
    @ApiResponse(responseCode = "200", description = "Share link validation result")
    public ResponseEntity<ShareLinkDto> validateShareLink(@PathVariable String token) {
        Optional<ShareLinkDto> shareLink = shareService.getShareLinkByToken(token);
        
        if (shareLink.isPresent()) {
            ShareLinkDto link = shareLink.get();
            
            // Check if the link has expired
            if (link.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                return ResponseEntity.status(HttpStatus.GONE).build();
            }
            
            return ResponseEntity.ok(link);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
