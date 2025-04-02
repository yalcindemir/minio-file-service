package com.fileservice.minioservice.service;

import com.fileservice.minioservice.dto.ShareLinkDto;
import com.fileservice.minioservice.exception.FileServiceException;
import com.fileservice.minioservice.model.FileEntity;
import com.fileservice.minioservice.model.ShareLink;
import com.fileservice.minioservice.repository.FileRepository;
import com.fileservice.minioservice.repository.ShareLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShareServiceTest {

    @Mock
    private ShareLinkRepository shareLinkRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private MinioService minioService;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private ShareService shareService;

    private FileEntity testFile;
    private ShareLink testShareLink;
    private final String TEST_USERNAME = "testuser";
    private final String TEST_TOKEN = "test-token-uuid";

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

        // Setup test share link
        testShareLink = new ShareLink();
        testShareLink.setId(1L);
        testShareLink.setToken(TEST_TOKEN);
        testShareLink.setFile(testFile);
        testShareLink.setPermission("READ");
        testShareLink.setCreatedAt(LocalDateTime.now());
        testShareLink.setExpiresAt(LocalDateTime.now().plusDays(7));
        testShareLink.setCreatedBy(TEST_USERNAME);
    }

    @Test
    void createShareLink_Success() {
        // Arrange
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFile));
        when(shareLinkRepository.save(any(ShareLink.class))).thenReturn(testShareLink);
        when(minioService.generatePresignedUrl(anyString(), anyInt())).thenReturn("https://minio-server/presigned-url");

        // Act
        ShareLinkDto result = shareService.createShareLink(1L, "READ", 7, TEST_USERNAME);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TOKEN, result.getToken());
        assertEquals(1L, result.getFileId());
        assertEquals("READ", result.getPermission());
        assertNotNull(result.getFileUrl());
        verify(fileRepository).findById(1L);
        verify(shareLinkRepository).save(any(ShareLink.class));
        verify(minioService).generatePresignedUrl(anyString(), anyInt());
    }

    @Test
    void createShareLink_FileNotFound() {
        // Arrange
        when(fileRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FileServiceException.FileNotFoundException.class, () -> {
            shareService.createShareLink(999L, "READ", 7, TEST_USERNAME);
        });
        verify(fileRepository).findById(999L);
        verifyNoInteractions(shareLinkRepository);
        verifyNoInteractions(minioService);
    }

    @Test
    void createShareLink_AccessDenied() {
        // Arrange
        FileEntity otherUserFile = testFile;
        otherUserFile.setOwner("otheruser");
        
        when(fileRepository.findById(1L)).thenReturn(Optional.of(otherUserFile));
        when(authorizationService.canReadFile(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(FileServiceException.FileAccessDeniedException.class, () -> {
            shareService.createShareLink(1L, "READ", 7, TEST_USERNAME);
        });
        verify(fileRepository).findById(1L);
        verify(authorizationService).canReadFile(anyString(), anyString());
        verifyNoInteractions(shareLinkRepository);
        verifyNoInteractions(minioService);
    }

    @Test
    void getShareLinkByToken_Success() {
        // Arrange
        when(shareLinkRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(testShareLink));
        when(minioService.generatePresignedUrl(anyString(), anyInt())).thenReturn("https://minio-server/presigned-url");

        // Act
        Optional<ShareLinkDto> result = shareService.getShareLinkByToken(TEST_TOKEN);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(TEST_TOKEN, result.get().getToken());
        assertEquals(1L, result.get().getFileId());
        verify(shareLinkRepository).findByToken(TEST_TOKEN);
        verify(minioService).generatePresignedUrl(anyString(), anyInt());
    }

    @Test
    void getShareLinkByToken_NotFound() {
        // Arrange
        when(shareLinkRepository.findByToken("non-existent-token")).thenReturn(Optional.empty());

        // Act
        Optional<ShareLinkDto> result = shareService.getShareLinkByToken("non-existent-token");

        // Assert
        assertFalse(result.isPresent());
        verify(shareLinkRepository).findByToken("non-existent-token");
        verifyNoInteractions(minioService);
    }

    @Test
    void getShareLinksForFile_Success() {
        // Arrange
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFile));
        when(shareLinkRepository.findByFileId(1L)).thenReturn(Arrays.asList(testShareLink));
        when(minioService.generatePresignedUrl(anyString(), anyInt())).thenReturn("https://minio-server/presigned-url");

        // Act
        List<ShareLinkDto> result = shareService.getShareLinksForFile(1L, TEST_USERNAME);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_TOKEN, result.get(0).getToken());
        verify(fileRepository).findById(1L);
        verify(shareLinkRepository).findByFileId(1L);
        verify(minioService).generatePresignedUrl(anyString(), anyInt());
    }

    @Test
    void validateShareLink_Success() {
        // Arrange
        when(shareLinkRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(testShareLink));

        // Act
        FileEntity result = shareService.validateShareLink(TEST_TOKEN, "READ");

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test.jpg", result.getFilename());
        verify(shareLinkRepository).findByToken(TEST_TOKEN);
    }

    @Test
    void validateShareLink_NotFound() {
        // Arrange
        when(shareLinkRepository.findByToken("non-existent-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FileServiceException.FileNotFoundException.class, () -> {
            shareService.validateShareLink("non-existent-token", "READ");
        });
        verify(shareLinkRepository).findByToken("non-existent-token");
    }

    @Test
    void validateShareLink_Expired() {
        // Arrange
        ShareLink expiredLink = testShareLink;
        expiredLink.setExpiresAt(LocalDateTime.now().minusDays(1));
        
        when(shareLinkRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(expiredLink));
        doNothing().when(shareLinkRepository).delete(any(ShareLink.class));

        // Act & Assert
        assertThrows(FileServiceException.FileAccessDeniedException.class, () -> {
            shareService.validateShareLink(TEST_TOKEN, "READ");
        });
        verify(shareLinkRepository).findByToken(TEST_TOKEN);
        verify(shareLinkRepository).delete(any(ShareLink.class));
    }

    @Test
    void validateShareLink_InsufficientPermission() {
        // Arrange
        ShareLink readOnlyLink = testShareLink;
        readOnlyLink.setPermission("READ");
        
        when(shareLinkRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(readOnlyLink));

        // Act & Assert
        assertThrows(FileServiceException.FileAccessDeniedException.class, () -> {
            shareService.validateShareLink(TEST_TOKEN, "WRITE");
        });
        verify(shareLinkRepository).findByToken(TEST_TOKEN);
    }
}
