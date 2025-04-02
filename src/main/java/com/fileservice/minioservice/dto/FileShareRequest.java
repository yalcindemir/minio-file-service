package com.fileservice.minioservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileShareRequest {
    private Long fileId;
    private String username;
    private String permission; // READ, WRITE, DELETE
    private Integer expiryDays;
}
