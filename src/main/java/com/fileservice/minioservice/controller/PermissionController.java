package com.fileservice.minioservice.controller;

import com.fileservice.minioservice.dto.PermissionRequest;
import com.fileservice.minioservice.model.FgaRelation;
import com.fileservice.minioservice.model.FileEntity;
import com.fileservice.minioservice.repository.FileRepository;
import com.fileservice.minioservice.service.AuthorizationService;
import com.fileservice.minioservice.exception.FileServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Permission Management", description = "API endpoints for managing file permissions")
@SecurityRequirement(name = "bearerAuth")
public class PermissionController {

    private final AuthorizationService authorizationService;
    private final FileRepository fileRepository;

    @GetMapping("/files/{fileId}")
    @Operation(
        summary = "Get all permissions for a file",
        description = "Retrieve all permission relations for a specific file"
    )
    @ApiResponse(responseCode = "200", description = "Permissions retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<List<FgaRelation>> getFilePermissions(
            @PathVariable Long fileId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt.getSubject();
        
        // Verify file exists
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileServiceException.FileNotFoundException("File not found with ID: " + fileId));
        
        // Check if user is the owner or has read permission
        if (!fileEntity.getOwner().equals(username) && !authorizationService.canReadFile(fileId.toString(), username)) {
            throw new FileServiceException.FileAccessDeniedException("You don't have permission to view this file's permissions");
        }
        
        List<FgaRelation> relations = authorizationService.getRelationsForObject("file", fileId.toString());
        return ResponseEntity.ok(relations);
    }

    @PostMapping
    @Operation(
        summary = "Grant permissions to a user",
        description = "Grant specific permissions on a file to another user"
    )
    @ApiResponse(responseCode = "200", description = "Permissions granted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<Void> grantPermissions(
            @RequestBody @Valid PermissionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String currentUsername = jwt.getSubject();
        
        // Verify file exists
        FileEntity fileEntity = fileRepository.findById(request.getFileId())
                .orElseThrow(() -> new FileServiceException.FileNotFoundException("File not found with ID: " + request.getFileId()));
        
        // Check if current user is the owner or has write permission
        if (!fileEntity.getOwner().equals(currentUsername) && !authorizationService.canWriteFile(request.getFileId().toString(), currentUsername)) {
            throw new FileServiceException.FileAccessDeniedException("You don't have permission to modify this file's permissions");
        }
        
        // Grant requested permissions
        String fileId = request.getFileId().toString();
        String targetUsername = request.getUsername();
        
        if (request.getPermissions() != null) {
            for (String permission : request.getPermissions()) {
                switch (permission.toUpperCase()) {
                    case "READ":
                        authorizationService.grantReadPermission(fileId, targetUsername);
                        break;
                    case "WRITE":
                        authorizationService.grantWritePermission(fileId, targetUsername);
                        break;
                    case "DELETE":
                        authorizationService.grantDeletePermission(fileId, targetUsername);
                        break;
                    default:
                        log.warn("Unknown permission type: {}", permission);
                }
            }
        }
        
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @Operation(
        summary = "Revoke permissions from a user",
        description = "Revoke specific permissions on a file from another user"
    )
    @ApiResponse(responseCode = "200", description = "Permissions revoked successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<Void> revokePermissions(
            @RequestBody @Valid PermissionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String currentUsername = jwt.getSubject();
        
        // Verify file exists
        FileEntity fileEntity = fileRepository.findById(request.getFileId())
                .orElseThrow(() -> new FileServiceException.FileNotFoundException("File not found with ID: " + request.getFileId()));
        
        // Check if current user is the owner
        if (!fileEntity.getOwner().equals(currentUsername)) {
            throw new FileServiceException.FileAccessDeniedException("Only the file owner can revoke permissions");
        }
        
        // Revoke requested permissions
        String fileId = request.getFileId().toString();
        String targetUsername = request.getUsername();
        
        if (request.getPermissions() != null) {
            for (String permission : request.getPermissions()) {
                switch (permission.toUpperCase()) {
                    case "READ":
                        authorizationService.revokeReadPermission(fileId, targetUsername);
                        break;
                    case "WRITE":
                        authorizationService.revokeWritePermission(fileId, targetUsername);
                        break;
                    case "DELETE":
                        authorizationService.revokeDeletePermission(fileId, targetUsername);
                        break;
                    default:
                        log.warn("Unknown permission type: {}", permission);
                }
            }
        }
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-access")
    @Operation(
        summary = "Get all files the current user has access to",
        description = "Retrieve all file relations for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Access information retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<List<FgaRelation>> getUserAccessibleFiles(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();
        List<FgaRelation> relations = authorizationService.getRelationsForUser(username);
        return ResponseEntity.ok(relations);
    }
}
