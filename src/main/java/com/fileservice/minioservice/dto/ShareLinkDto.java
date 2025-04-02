package com.fileservice.minioservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLinkDto {
    private String token;
    private Long fileId;
    private String fileUrl;
    private LocalDateTime expiresAt;
    private String permission; // READ, WRITE
}
