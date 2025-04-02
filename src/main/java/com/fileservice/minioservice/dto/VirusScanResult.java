package com.fileservice.minioservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirusScanResult {
    private boolean scanned;
    private boolean clean;
    private int positives;
    private int total;
    private String scanId;
    private String resource;
    private String permalink;
    private Map<String, Object> scanDetails;
    private String message;
}
