package com.fileservice.minioservice.service;

import com.fileservice.minioservice.config.VirusTotalConfig;
import com.fileservice.minioservice.dto.VirusScanResult;
import com.fileservice.minioservice.exception.FileServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VirusScanService {

    private final VirusTotalConfig virusTotalConfig;
    private final RestTemplate virusTotalRestTemplate;

    /**
     * Scan a file for viruses using VirusTotal API
     */
    public VirusScanResult scanFile(MultipartFile file) {
        if (!virusTotalConfig.isEnabled()) {
            log.info("VirusTotal scanning is disabled");
            return VirusScanResult.builder()
                    .scanned(false)
                    .clean(true)
                    .message("Virus scanning is disabled")
                    .build();
        }

        try {
            // First upload the file to VirusTotal
            String scanId = uploadFileToVirusTotal(file);
            
            // Then get the scan report
            return getFileScanReport(scanId);
        } catch (Exception e) {
            log.error("Error scanning file with VirusTotal: {}", e.getMessage(), e);
            return VirusScanResult.builder()
                    .scanned(false)
                    .clean(false)
                    .message("Error scanning file: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Upload a file to VirusTotal for scanning
     */
    private String uploadFileToVirusTotal(MultipartFile file) throws IOException {
        String url = virusTotalConfig.getApiUrl() + "/files";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("x-apikey", virusTotalConfig.getApiKey());
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        body.add("file", fileResource);
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<Map> response = virusTotalRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    return (String) data.get("id");
                }
            }
            
            throw new FileServiceException("Failed to upload file to VirusTotal", "VIRUS_SCAN_FAILED");
        } catch (RestClientException e) {
            log.error("Error uploading file to VirusTotal: {}", e.getMessage(), e);
            throw new FileServiceException("Failed to upload file to VirusTotal: " + e.getMessage(), "VIRUS_SCAN_FAILED", e);
        }
    }

    /**
     * Get the scan report for a file from VirusTotal
     */
    private VirusScanResult getFileScanReport(String scanId) {
        String url = virusTotalConfig.getApiUrl() + "/analyses/" + scanId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apikey", virusTotalConfig.getApiKey());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        
        try {
            // Wait for the scan to complete (with timeout)
            long startTime = System.currentTimeMillis();
            boolean scanComplete = false;
            ResponseEntity<Map> response = null;
            
            while (!scanComplete && (System.currentTimeMillis() - startTime) < virusTotalConfig.getScanTimeout()) {
                response = virusTotalRestTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        Map.class
                );
                
                if (response.getStatusCode() == HttpStatus.OK) {
                    Map<String, Object> responseBody = response.getBody();
                    if (responseBody != null && responseBody.containsKey("data")) {
                        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                        Map<String, Object> attributes = (Map<String, Object>) data.get("attributes");
                        String status = (String) attributes.get("status");
                        
                        if ("completed".equals(status)) {
                            scanComplete = true;
                        } else {
                            // Wait before checking again
                            Thread.sleep(2000);
                        }
                    }
                }
            }
            
            if (response != null && response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    Map<String, Object> attributes = (Map<String, Object>) data.get("attributes");
                    Map<String, Object> stats = (Map<String, Object>) attributes.get("stats");
                    Map<String, Object> results = (Map<String, Object>) attributes.get("results");
                    
                    int malicious = ((Number) stats.get("malicious")).intValue();
                    int total = malicious + ((Number) stats.get("undetected")).intValue();
                    
                    return VirusScanResult.builder()
                            .scanned(true)
                            .clean(malicious == 0)
                            .positives(malicious)
                            .total(total)
                            .scanId(scanId)
                            .resource((String) data.get("id"))
                            .permalink((String) attributes.get("permalink"))
                            .scanDetails(results)
                            .message(malicious > 0 ? "Virus detected" : "No virus detected")
                            .build();
                }
            }
            
            return VirusScanResult.builder()
                    .scanned(false)
                    .clean(false)
                    .message("Failed to get scan results from VirusTotal")
                    .build();
        } catch (Exception e) {
            log.error("Error getting scan report from VirusTotal: {}", e.getMessage(), e);
            return VirusScanResult.builder()
                    .scanned(false)
                    .clean(false)
                    .message("Error getting scan results: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Check if a file is safe based on scan results
     */
    public boolean isFileSafe(VirusScanResult scanResult) {
        // If scanning is disabled or failed, we assume the file is safe
        if (!scanResult.isScanned()) {
            return true;
        }
        
        return scanResult.isClean();
    }
}
