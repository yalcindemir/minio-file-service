package com.fileservice.minioservice.controller;

import com.fileservice.minioservice.dto.ImageDimension;
import com.fileservice.minioservice.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Image Operations", description = "API endpoints for image processing operations")
@SecurityRequirement(name = "bearerAuth")
public class ImageController {

    private final ImageService imageService;

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Validate if file is an image",
        description = "Check if the uploaded file is a valid image that can be processed"
    )
    @ApiResponse(responseCode = "200", description = "Validation result")
    public ResponseEntity<Boolean> validateImage(@RequestParam("file") MultipartFile file) {
        boolean isImage = imageService.isImage(file.getContentType());
        return ResponseEntity.ok(isImage);
    }

    @GetMapping("/dimensions/presets")
    @Operation(
        summary = "Get preset image dimensions",
        description = "Retrieve a list of preset image dimensions for thumbnails"
    )
    @ApiResponse(responseCode = "200", description = "List of preset dimensions")
    public ResponseEntity<List<ImageDimension>> getPresetDimensions() {
        List<ImageDimension> presets = new ArrayList<>();
        presets.add(new ImageDimension(16, 16));
        presets.add(new ImageDimension(32, 32));
        presets.add(new ImageDimension(64, 64));
        presets.add(new ImageDimension(128, 128));
        presets.add(new ImageDimension(256, 256));
        presets.add(new ImageDimension(512, 512));
        return ResponseEntity.ok(presets);
    }
}
