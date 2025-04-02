package com.fileservice.minioservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDimension {
    
    @NotNull(message = "Width is required")
    @Min(value = 1, message = "Width must be at least 1 pixel")
    private Integer width;
    
    @NotNull(message = "Height is required")
    @Min(value = 1, message = "Height must be at least 1 pixel")
    private Integer height;
}
