package com.fileservice.minioservice.service;

import com.fileservice.minioservice.dto.FileDto;
import com.fileservice.minioservice.dto.FileUploadRequest;
import com.fileservice.minioservice.dto.ImageDimension;
import com.fileservice.minioservice.exception.FileServiceException;
import com.fileservice.minioservice.model.FileEntity;
import com.fileservice.minioservice.repository.FileRepository;
import io.minio.GetObjectResponse;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileServiceTest {

    @Mock
    private MinioService minioService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private FileService fileService;

    private FileEntity testFile;
    private FileDto testFileDto;
    private MultipartFile mockMultipartFile;
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
        testFile.setThumbnailPaths(new HashSet<>(Arrays.asList("/fileservice/test-uuid_test_64x64.jpg")));

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
        testFileDto.setThumbnailPaths(new HashSet<>(Arrays.asList("/fileservice/test-uuid_test_64x64.jpg")));
        testFileDto.setDownloadUrl("https://minio-server/fileservice/test-uuid_test.jpg?token=xyz");

        // Setup mock multipart file
        mockMultipartFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
    }

    @Test
    void uploadFile_Success() throws IOException {
        // Arrange
        FileUploadRequest request = new FileUploadRequest();
        request.setFile(mockMultipartFile);
        request.setExpiryDays(7);
        
        List<ImageDimension> dimensions = new ArrayList<>();
        dimensions.add(new ImageDimension(64, 64));
        request.setThumbnailDimensions(dimensions);

        when(minioService.uploadFile(any(FileUploadRequest.class), eq(TEST_USERNAME))).thenReturn(testFile);
        when(minioService.convertToDto(any(FileEntity.class), anyInt())).thenReturn(testFileDto);

        // Act
        FileDto result = fileService.uploadFile(request, TEST_USERNAME);

        // Assert
        assertNotNull(result);
        assertEquals(testFileDto.getId(), result.getId());
        assertEquals(testFileDto.getFilename(), result.getFilename());
        assertEquals(testFileDto.getDownloadUrl(), result.getDownloadUrl());
        verify(minioService).uploadFile(any(FileUploadRequest.class), eq(TEST_USERNAME));
        verify(minioService).convertToDto(any(FileEntity.class), anyInt());
    }

    @Test
    void getFileById_Success() {
        // Arrange
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFile));
        when(minioService.convertToDto(any(FileEntity.class), anyInt())).thenReturn(testFileDto);

        // Act
        FileDto result = fileService.getFileById(1L, TEST_USERNAME);

        // Assert
        assertNotNull(result);
        assertEquals(testFileDto.getId(), result.getId());
        assertEquals(testFileDto.getFilename(), result.getFilename());
        verify(fileRepository).findById(1L);
        verify(minioService).convertToDto(any(FileEntity.class), anyInt());
    }

    @Test
    void getFileById_NotFound() {
        // Arrange
        when(fileRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FileServiceException.FileNotFoundException.class, () -> {
            fileService.getFileById(999L, TEST_USERNAME);
        });
        verify(fileRepository).findById(999L);
    }

    @Test
    void getFileById_AccessDenied() {
        // Arrange
        FileEntity otherUserFile = testFile;
        otherUserFile.setOwner("otheruser");
        
        when(fileRepository.findById(1L)).thenReturn(Optional.of(otherUserFile));

        // Act & Assert
        assertThrows(FileServiceException.FileAccessDeniedException.class, () -> {
            fileService.getFileById(1L, TEST_USERNAME);
        });
        verify(fileRepository).findById(1L);
    }

    @Test
    void getUserFiles_Success() {
        // Arrange
        List<FileEntity> userFiles = Arrays.asList(testFile);
        when(fileRepository.findByOwner(TEST_USERNAME)).thenReturn(userFiles);
        when(minioService.convertToDto(any(FileEntity.class), anyInt())).thenReturn(testFileDto);

        // Act
        List<FileDto> result = fileService.getUserFiles(TEST_USERNAME);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testFileDto.getId(), result.get(0).getId());
        verify(fileRepository).findByOwner(TEST_USERNAME);
        verify(minioService).convertToDto(any(FileEntity.class), anyInt());
    }

    @Test
    void deleteFile_Success() {
        // Arrange
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFile));
        doNothing().when(minioService).deleteFile(anyString());
        doNothing().when(fileRepository).delete(any(FileEntity.class));

        // Act
        fileService.deleteFile(1L, TEST_USERNAME);

        // Assert
        verify(fileRepository).findById(1L);
        verify(minioService).deleteFile(testFile.getObjectName());
        verify(fileRepository).delete(testFile);
    }

    @Test
    void updateFileMetadata_Success() {
        // Arrange
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFile));
        when(fileRepository.save(any(FileEntity.class))).thenReturn(testFile);
        when(minioService.convertToDto(any(FileEntity.class), anyInt())).thenReturn(testFileDto);

        // Act
        FileDto result = fileService.updateFileMetadata(1L, "updated.jpg", 14, TEST_USERNAME);

        // Assert
        assertNotNull(result);
        verify(fileRepository).findById(1L);
        verify(fileRepository).save(any(FileEntity.class));
        verify(minioService).convertToDto(any(FileEntity.class), anyInt());
    }
}
