package com.fileservice.minioservice.service;

import com.fileservice.minioservice.config.VirusTotalConfig;
import com.fileservice.minioservice.dto.VirusScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VirusScanServiceTest {

    @Mock
    private VirusTotalConfig virusTotalConfig;

    @Mock
    private RestTemplate virusTotalRestTemplate;

    @InjectMocks
    private VirusScanService virusScanService;

    private MultipartFile mockFile;
    private Map<String, Object> uploadResponse;
    private Map<String, Object> scanResponse;

    @BeforeEach
    void setUp() {
        // Setup mock file
        mockFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );

        // Setup mock upload response
        uploadResponse = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("id", "test-scan-id");
        uploadResponse.put("data", data);

        // Setup mock scan response
        scanResponse = new HashMap<>();
        Map<String, Object> scanData = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();
        Map<String, Object> stats = new HashMap<>();
        stats.put("malicious", 0);
        stats.put("undetected", 10);
        attributes.put("stats", stats);
        attributes.put("status", "completed");
        attributes.put("permalink", "https://virustotal.com/test-scan-id");
        Map<String, Object> results = new HashMap<>();
        attributes.put("results", results);
        scanData.put("attributes", attributes);
        scanData.put("id", "test-resource-id");
        scanResponse.put("data", scanData);
    }

    @Test
    void scanFile_WhenDisabled_ReturnsNotScannedResult() {
        // Arrange
        when(virusTotalConfig.isEnabled()).thenReturn(false);

        // Act
        VirusScanResult result = virusScanService.scanFile(mockFile);

        // Assert
        assertFalse(result.isScanned());
        assertTrue(result.isClean());
        assertEquals("Virus scanning is disabled", result.getMessage());
        verifyNoInteractions(virusTotalRestTemplate);
    }

    @Test
    void scanFile_WhenEnabled_ReturnsCleanResult() {
        // Arrange
        when(virusTotalConfig.isEnabled()).thenReturn(true);
        when(virusTotalConfig.getApiUrl()).thenReturn("https://virustotal.com/api/v3");
        when(virusTotalConfig.getApiKey()).thenReturn("test-api-key");
        when(virusTotalConfig.getScanTimeout()).thenReturn(60000);

        ResponseEntity<Map> uploadResponseEntity = new ResponseEntity<>(uploadResponse, HttpStatus.OK);
        ResponseEntity<Map> scanResponseEntity = new ResponseEntity<>(scanResponse, HttpStatus.OK);

        when(virusTotalRestTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(uploadResponseEntity);

        when(virusTotalRestTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(scanResponseEntity);

        // Act
        VirusScanResult result = virusScanService.scanFile(mockFile);

        // Assert
        assertTrue(result.isScanned());
        assertTrue(result.isClean());
        assertEquals(0, result.getPositives());
        assertEquals(10, result.getTotal());
        assertEquals("test-scan-id", result.getScanId());
        assertEquals("test-resource-id", result.getResource());
        assertEquals("https://virustotal.com/test-scan-id", result.getPermalink());
        assertEquals("No virus detected", result.getMessage());
        
        verify(virusTotalRestTemplate).exchange(
                contains("/files"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        );
        
        verify(virusTotalRestTemplate).exchange(
                contains("/analyses/test-scan-id"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }

    @Test
    void scanFile_WhenVirusDetected_ReturnsInfectedResult() {
        // Arrange
        when(virusTotalConfig.isEnabled()).thenReturn(true);
        when(virusTotalConfig.getApiUrl()).thenReturn("https://virustotal.com/api/v3");
        when(virusTotalConfig.getApiKey()).thenReturn("test-api-key");
        when(virusTotalConfig.getScanTimeout()).thenReturn(60000);

        // Modify stats to indicate virus detection
        Map<String, Object> infectedScanResponse = new HashMap<>(scanResponse);
        Map<String, Object> infectedScanData = new HashMap<>((Map<String, Object>) infectedScanResponse.get("data"));
        Map<String, Object> infectedAttributes = new HashMap<>((Map<String, Object>) infectedScanData.get("attributes"));
        Map<String, Object> infectedStats = new HashMap<>();
        infectedStats.put("malicious", 3);
        infectedStats.put("undetected", 7);
        infectedAttributes.put("stats", infectedStats);
        infectedScanData.put("attributes", infectedAttributes);
        infectedScanResponse.put("data", infectedScanData);

        ResponseEntity<Map> uploadResponseEntity = new ResponseEntity<>(uploadResponse, HttpStatus.OK);
        ResponseEntity<Map> scanResponseEntity = new ResponseEntity<>(infectedScanResponse, HttpStatus.OK);

        when(virusTotalRestTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(uploadResponseEntity);

        when(virusTotalRestTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(scanResponseEntity);

        // Act
        VirusScanResult result = virusScanService.scanFile(mockFile);

        // Assert
        assertTrue(result.isScanned());
        assertFalse(result.isClean());
        assertEquals(3, result.getPositives());
        assertEquals(10, result.getTotal());
        assertEquals("Virus detected", result.getMessage());
    }

    @Test
    void isFileSafe_WhenNotScanned_ReturnsTrue() {
        // Arrange
        VirusScanResult result = VirusScanResult.builder()
                .scanned(false)
                .clean(false)
                .message("Scanning disabled")
                .build();

        // Act
        boolean isSafe = virusScanService.isFileSafe(result);

        // Assert
        assertTrue(isSafe);
    }

    @Test
    void isFileSafe_WhenScannedAndClean_ReturnsTrue() {
        // Arrange
        VirusScanResult result = VirusScanResult.builder()
                .scanned(true)
                .clean(true)
                .positives(0)
                .total(10)
                .message("No virus detected")
                .build();

        // Act
        boolean isSafe = virusScanService.isFileSafe(result);

        // Assert
        assertTrue(isSafe);
    }

    @Test
    void isFileSafe_WhenScannedAndInfected_ReturnsFalse() {
        // Arrange
        VirusScanResult result = VirusScanResult.builder()
                .scanned(true)
                .clean(false)
                .positives(3)
                .total(10)
                .message("Virus detected")
                .build();

        // Act
        boolean isSafe = virusScanService.isFileSafe(result);

        // Assert
        assertFalse(isSafe);
    }
}
