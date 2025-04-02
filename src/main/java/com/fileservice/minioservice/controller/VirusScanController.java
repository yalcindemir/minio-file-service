package com.fileservice.minioservice.controller;

import com.fileservice.minioservice.dto.FileDto;
import com.fileservice.minioservice.model.VirusScanEntity;
import com.fileservice.minioservice.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/virus-scan")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Virus Scanning", description = "API endpoints for virus scanning operations")
@SecurityRequirement(name = "bearerAuth")
public class VirusScanController {

    private final FileService fileService;

    @GetMapping("/files/{fileId}")
    @Operation(
        summary = "Get virus scan result for a file",
        description = "Retrieve the virus scan result for a specific file by its ID"
    )
    @ApiResponse(responseCode = "200", description = "Scan result retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "File or scan result not found")
    public ResponseEntity<VirusScanEntity> getVirusScanResult(
            @PathVariable Long fileId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = jwt.getSubject();
        
        // First check if user has access to the file
        FileDto fileDto = fileService.getFileById(fileId, username);
        
        // Then get the virus scan result
        Optional<VirusScanEntity> scanResult = fileService.getVirusScanResult(fileId);
        
        return scanResult
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
