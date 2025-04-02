package com.fileservice.minioservice.controller;

import com.fileservice.minioservice.dto.FileDto;
import com.fileservice.minioservice.dto.FileUploadRequest;
import com.fileservice.minioservice.dto.ImageDimension;
import com.fileservice.minioservice.service.FileService;
import io.minio.GetObjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Operations", description = "API endpoints for file management operations")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload a file",
        description = "Upload a file to the system with optional custom filename, expiry days, and thumbnail dimensions"
    )
    @ApiResponse(responseCode = "201", description = "File uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<FileDto> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "customFilename", required = false) String customFilename,
            @RequestParam(value = "expiryDays", required = false) Integer expiryDays,
            @RequestParam(value = "thumbnailDimensions", required = false) List<ImageDimension> thumbnailDimensions,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt.getSubject();
        
        FileUploadRequest request = FileUploadRequest.builder()
                .file(file)
                .customFilename(customFilename)
                .expiryDays(expiryDays)
                .thumbnailDimensions(thumbnailDimensions)
                .build();
        
        FileDto uploadedFile = fileService.uploadFile(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedFile);
    }

    @GetMapping
    @Operation(
        summary = "Get all user files",
        description = "Retrieve all files owned by the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Files retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<List<FileDto>> getUserFiles(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();
        List<FileDto> userFiles = fileService.getUserFiles(username);
        return ResponseEntity.ok(userFiles);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get file metadata by ID",
        description = "Retrieve file metadata for a specific file by its ID"
    )
    @ApiResponse(responseCode = "200", description = "File metadata retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<FileDto> getFileById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt.getSubject();
        FileDto file = fileService.getFileById(id, username);
        return ResponseEntity.ok(file);
    }

    @GetMapping("/{id}/content")
    @Operation(
        summary = "Download file content by ID",
        description = "Download the actual file content for a specific file by its ID"
    )
    @ApiResponse(responseCode = "200", description = "File content retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<InputStreamResource> getFileContent(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt.getSubject();
        Optional<GetObjectResponse> fileContent = fileService.getFileContent(id, username);
        
        if (fileContent.isPresent()) {
            GetObjectResponse response = fileContent.get();
            FileDto fileDto = fileService.getFileById(id, username);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(fileDto.getContentType()));
            headers.setContentDispositionFormData("attachment", fileDto.getFilename());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(response));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete file by ID",
        description = "Delete a specific file by its ID"
    )
    @ApiResponse(responseCode = "204", description = "File deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt.getSubject();
        fileService.deleteFile(id, username);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Update file metadata",
        description = "Update metadata for a specific file by its ID"
    )
    @ApiResponse(responseCode = "200", description = "File metadata updated successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<FileDto> updateFileMetadata(
            @PathVariable Long id,
            @RequestParam(value = "filename", required = false) String newFilename,
            @RequestParam(value = "expiryDays", required = false) Integer newExpiryDays,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt.getSubject();
        FileDto updatedFile = fileService.updateFileMetadata(id, newFilename, newExpiryDays, username);
        return ResponseEntity.ok(updatedFile);
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search files by filename",
        description = "Search for files by filename pattern"
    )
    @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<List<FileDto>> searchFiles(
            @RequestParam("filename") String filenamePattern,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt.getSubject();
        List<FileDto> searchResults = fileService.searchFilesByFilename(filenamePattern, username);
        return ResponseEntity.ok(searchResults);
    }
}
