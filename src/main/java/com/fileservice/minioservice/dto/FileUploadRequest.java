package com.fileservice.minioservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequest {
    
    @NotNull(message = "File is required")
    private MultipartFile file;
    
    private String customFilename;
    
    @Min(value = 1, message = "Expiry days must be at least 1")
    private Integer expiryDays;
    
    private List<ImageDimension> thumbnailDimensions;
}
