package com.fileservice.minioservice.service;

import com.fileservice.minioservice.dto.ImageDimension;
import com.fileservice.minioservice.exception.FileServiceException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ImageServiceTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private ImageService imageService;

    private MultipartFile mockImageFile;
    private MultipartFile mockNonImageFile;
    private List<ImageDimension> dimensions;

    @BeforeEach
    void setUp() {
        // Setup mock image file
        mockImageFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // Setup mock non-image file
        mockNonImageFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test text content".getBytes()
        );

        // Setup dimensions
        dimensions = new ArrayList<>();
        dimensions.add(new ImageDimension(64, 64));
        dimensions.add(new ImageDimension(128, 128));
    }

    @Test
    void isImage_ReturnsTrueForImageFile() {
        // Act
        boolean result = imageService.isImage(mockImageFile.getContentType());

        // Assert
        assertTrue(result);
    }

    @Test
    void isImage_ReturnsFalseForNonImageFile() {
        // Act
        boolean result = imageService.isImage(mockNonImageFile.getContentType());

        // Assert
        assertFalse(result);
    }

    @Test
    void isImage_ReturnsFalseForNullContentType() {
        // Act
        boolean result = imageService.isImage(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void generateThumbnails_ReturnsEmptySetForNonImageFile() {
        // Act
        Set<String> result = imageService.generateThumbnails(mockNonImageFile, dimensions, "test-uuid_test.txt");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(minioClient);
    }

    @Test
    void generateThumbnails_ReturnsEmptySetForNullDimensions() {
        // Act
        Set<String> result = imageService.generateThumbnails(mockImageFile, null, "test-uuid_test.jpg");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(minioClient);
    }

    @Test
    void generateThumbnails_ReturnsEmptySetForEmptyDimensions() {
        // Act
        Set<String> result = imageService.generateThumbnails(mockImageFile, new ArrayList<>(), "test-uuid_test.jpg");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(minioClient);
    }

    @Test
    void generateThumbnails_Success() throws Exception {
        // Arrange
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        // Act
        Set<String> result = imageService.generateThumbnails(mockImageFile, dimensions, "test-uuid_test.jpg");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(minioClient, times(2)).putObject(any(PutObjectArgs.class));
    }
}
