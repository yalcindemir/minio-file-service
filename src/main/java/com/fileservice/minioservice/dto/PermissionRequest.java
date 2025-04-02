package com.fileservice.minioservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequest {
    
    @NotNull(message = "File ID is required")
    private Long fileId;
    
    @NotBlank(message = "Username is required")
    private String username;
    
    private Set<String> permissions; // READ, WRITE, DELETE
    
    private Integer expiryDays;
}
