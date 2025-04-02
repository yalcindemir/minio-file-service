package com.fileservice.minioservice.service;

import com.fileservice.minioservice.dto.FileDto;
import com.fileservice.minioservice.dto.FileUploadRequest;
import com.fileservice.minioservice.dto.VirusScanResult;
import com.fileservice.minioservice.exception.FileServiceException;
import com.fileservice.minioservice.model.FileEntity;
import com.fileservice.minioservice.model.VirusScanEntity;
import com.fileservice.minioservice.repository.FileRepository;
import com.fileservice.minioservice.repository.VirusScanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileServiceVirusScanTest {

    @Mock
    private MinioService minioService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private VirusScanService virusScanService;

    @Mock
    private VirusScanRepository virusScanRepository;

    @InjectMocks
    private FileService fileService;

    private FileEntity testFile;
    private FileDto testFileDto;
    private MultipartFile mockMultipartFile;
    private VirusScanResult cleanScanResult;
    private VirusScanResult infectedScanResult;
    private final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        // Setup test file entity
        testFile = new FileEntity();
        testFile.setId(1L);
        testFile.setFilename("test.jpg");
        testFile.setContentType("image/jpeg");
        testFile.setPath("/fileservice/test-uuid_test.jpg");
        testFile.setSize(1024L);
        testFile.setBucketName("fileservice");
        testFile.setObjectName("test-uuid_test.jpg");
        testFile.setOwner(TEST_USERNAME);
        testFile.setCreatedAt(LocalDateTime.now());
        testFile.setExpiresAt(LocalDateTime.now().plusDays(7));

        // Setup test file DTO
        testFileDto = new FileDto();
        testFileDto.setId(1L);
        testFileDto.setFilename("test.jpg");
        testFileDto.setContentType("image/jpeg");
        testFileDto.setPath("/fileservice/test-uuid_test.jpg");
        testFileDto.setSize(1024L);
        testFileDto.setOwner(TEST_USERNAME);
        testFileDto.setCreatedAt(LocalDateTime.now());
        testFileDto.setExpiresAt(LocalDateTime.now().plusDays(7));
        testFileDto.setDownloadUrl("https://minio-server/fileservice/test-uuid_test.jpg?token=xyz");

        // Setup mock multipart file
        mockMultipartFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // Setup clean scan result
        cleanScanResult = VirusScanResult.builder()
                .scanned(true)
                .clean(true)
                .positives(0)
                .total(10)
                .scanId("test-scan-id")
                .resource("test-resource-id")
                .permalink("https://virustotal.com/test-scan-id")
                .message("No virus detected")
                .build();

        // Setup infected scan result
        infectedScanResult = VirusScanResult.builder()
                .scanned(true)
                .clean(false)
                .positives(3)
                .total(10)
                .scanId("test-scan-id")
                .resource("test-resource-id")
                .permalink("https://virustotal.com/test-scan-id")
                .message("Virus detected")
                .build();
    }

    @Test
    void uploadFile_WithCleanFile_SuccessfullyUploads() throws IOException {
        // Arrange
        FileUploadRequest request = new FileUploadRequest();
        request.setFile(mockMultipartFile);
        request.setExpiryDays(7);

        when(virusScanService.scanFile(any(MultipartFile.class))).thenReturn(cleanScanResult);
        when(virusScanService.isFileSafe(any(VirusScanResult.class))).thenReturn(true);
        when(minioService.uploadFile(any(FileUploadRequest.class), eq(TEST_USERNAME))).thenReturn(testFile);
        when(minioService.convertToDto(any(FileEntity.class), anyInt())).thenReturn(testFileDto);
        doNothing().when(virusScanRepository).save(any(VirusScanEntity.class));

        // Act
        FileDto result = fileService.uploadFile(request, TEST_USERNAME);

        // Assert
        assertNotNull(result);
        assertEquals(testFileDto.getId(), result.getId());
        assertEquals(testFileDto.getFilename(), result.getFilename());
        verify(virusScanService).scanFile(any(MultipartFile.class));
        verify(virusScanService).isFileSafe(any(VirusScanResult.class));
        verify(minioService).uploadFile(any(FileUploadRequest.class), eq(TEST_USERNAME));
        verify(virusScanRepository).save(any(VirusScanEntity.class));
        verify(minioService).convertToDto(any(FileEntity.class), anyInt());
    }

    @Test
    void uploadFile_WithInfectedFile_ThrowsException() {
        // Arrange
        FileUploadRequest request = new FileUploadRequest();
        request.setFile(mockMultipartFile);
        request.setExpiryDays(7);

        when(virusScanService.scanFile(any(MultipartFile.class))).thenReturn(infectedScanResult);
        when(virusScanService.isFileSafe(any(VirusScanResult.class))).thenReturn(false);

        // Act & Assert
        FileServiceException exception = assertThrows(FileServiceException.InvalidFileTypeException.class, () -> {
            fileService.uploadFile(request, TEST_USERNAME);
        });

        assertTrue(exception.getMessage().contains("File contains malware"));
        verify(virusScanService).scanFile(any(MultipartFile.class));
        verify(virusScanService).isFileSafe(any(VirusScanResult.class));
        verifyNoInteractions(minioService);
        verifyNoInteractions(virusScanRepository);
    }

    @Test
    void getVirusScanResult_WhenExists_ReturnsResult() {
        // Arrange
        VirusScanEntity scanEntity = new VirusScanEntity();
        scanEntity.setId(1L);
        scanEntity.setFileId("1");
        scanEntity.setScanned(true);
        scanEntity.setClean(true);
        scanEntity.setPositives(0);
        scanEntity.setTotal(10);
        scanEntity.setScanId("test-scan-id");
        scanEntity.setResource("test-resource-id");
        scanEntity.setPermalink("https://virustotal.com/test-scan-id");
        scanEntity.setMessage("No virus detected");
        scanEntity.setScanDate(LocalDateTime.now());

        when(virusScanRepository.findByFileId("1")).thenReturn(Optional.of(scanEntity));

        // Act
        Optional<VirusScanEntity> result = fileService.getVirusScanResult(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(scanEntity.getId(), result.get().getId());
        assertEquals(scanEntity.getFileId(), result.get().getFileId());
        assertEquals(scanEntity.isClean(), result.get().isClean());
        verify(virusScanRepository).findByFileId("1");
    }

    @Test
    void getVirusScanResult_WhenNotExists_ReturnsEmpty() {
        // Arrange
        when(virusScanRepository.findByFileId("999")).thenReturn(Optional.empty());

        // Act
        Optional<VirusScanEntity> result = fileService.getVirusScanResult(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(virusScanRepository).findByFileId("999");
    }

    @Test
    void deleteFile_AlsoDeletesVirusScanResult() {
        // Arrange
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFile));
        doNothing().when(minioService).deleteFile(anyString());
        doNothing().when(virusScanRepository).deleteByFileId(anyString());
        doNothing().when(fileRepository).delete(any(FileEntity.class));

        // Act
        fileService.deleteFile(1L, TEST_USERNAME);

        // Assert
        verify(fileRepository).findById(1L);
        verify(minioService).deleteFile(testFile.getObjectName());
        verify(virusScanRepository).deleteByFileId("1");
        verify(fileRepository).delete(testFile);
    }
}
