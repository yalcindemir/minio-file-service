package com.fileservice.minioservice.service;

import com.fileservice.minioservice.model.FgaRelation;
import dev.openfga.sdk.api.client.OpenFgaClient;
import dev.openfga.sdk.api.client.model.*;
import dev.openfga.sdk.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthorizationServiceTest {

    @Mock
    private OpenFgaClient openFgaClient;

    @InjectMocks
    private AuthorizationService authorizationService;

    private final String TEST_USERNAME = "testuser";
    private final String TEST_FILE_ID = "123";

    @BeforeEach
    void setUp() {
    }

    @Test
    void checkPermission_Allowed() throws Exception {
        // Arrange
        CheckResponse checkResponse = new CheckResponse();
        checkResponse.setAllowed(true);
        
        CompletableFuture<CheckResponse> future = CompletableFuture.completedFuture(checkResponse);
        when(openFgaClient.check(any(ClientCheckRequest.class))).thenReturn(future);

        // Act
        boolean result = authorizationService.checkPermission("file", TEST_FILE_ID, "reader", TEST_USERNAME);

        // Assert
        assertTrue(result);
        verify(openFgaClient).check(any(ClientCheckRequest.class));
    }

    @Test
    void checkPermission_Denied() throws Exception {
        // Arrange
        CheckResponse checkResponse = new CheckResponse();
        checkResponse.setAllowed(false);
        
        CompletableFuture<CheckResponse> future = CompletableFuture.completedFuture(checkResponse);
        when(openFgaClient.check(any(ClientCheckRequest.class))).thenReturn(future);

        // Act
        boolean result = authorizationService.checkPermission("file", TEST_FILE_ID, "reader", TEST_USERNAME);

        // Assert
        assertFalse(result);
        verify(openFgaClient).check(any(ClientCheckRequest.class));
    }

    @Test
    void addRelation_Success() throws Exception {
        // Arrange
        WriteResponse writeResponse = new WriteResponse();
        CompletableFuture<WriteResponse> future = CompletableFuture.completedFuture(writeResponse);
        when(openFgaClient.write(any(ClientWriteRequest.class))).thenReturn(future);

        // Act
        authorizationService.addRelation("file", TEST_FILE_ID, "reader", TEST_USERNAME);

        // Assert
        verify(openFgaClient).write(any(ClientWriteRequest.class));
    }

    @Test
    void removeRelation_Success() throws Exception {
        // Arrange
        WriteResponse writeResponse = new WriteResponse();
        CompletableFuture<WriteResponse> future = CompletableFuture.completedFuture(writeResponse);
        when(openFgaClient.write(any(ClientWriteRequest.class))).thenReturn(future);

        // Act
        authorizationService.removeRelation("file", TEST_FILE_ID, "reader", TEST_USERNAME);

        // Assert
        verify(openFgaClient).write(any(ClientWriteRequest.class));
    }

    @Test
    void getRelationsForObject_Success() throws Exception {
        // Arrange
        Tuple tuple = new Tuple();
        TupleKey tupleKey = new TupleKey()
                .object("file:123")
                .relation("reader")
                .user("user:testuser");
        tuple.setKey(tupleKey);
        
        ReadResponse readResponse = new ReadResponse();
        readResponse.setTuples(Collections.singletonList(tuple));
        
        CompletableFuture<ReadResponse> future = CompletableFuture.completedFuture(readResponse);
        when(openFgaClient.read(any(ClientReadRequest.class))).thenReturn(future);

        // Act
        List<FgaRelation> result = authorizationService.getRelationsForObject("file", TEST_FILE_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("file:123", result.get(0).getObject());
        assertEquals("reader", result.get(0).getRelation());
        assertEquals("user:testuser", result.get(0).getUser());
        verify(openFgaClient).read(any(ClientReadRequest.class));
    }

    @Test
    void canReadFile_True() {
        // Arrange
        doReturn(true).when(authorizationService).checkPermission(eq("file"), eq(TEST_FILE_ID), eq("reader"), eq(TEST_USERNAME));

        // Act
        boolean result = authorizationService.canReadFile(TEST_FILE_ID, TEST_USERNAME);

        // Assert
        assertTrue(result);
    }

    @Test
    void canWriteFile_True() {
        // Arrange
        doReturn(true).when(authorizationService).checkPermission(eq("file"), eq(TEST_FILE_ID), eq("writer"), eq(TEST_USERNAME));

        // Act
        boolean result = authorizationService.canWriteFile(TEST_FILE_ID, TEST_USERNAME);

        // Assert
        assertTrue(result);
    }

    @Test
    void canDeleteFile_True() {
        // Arrange
        doReturn(true).when(authorizationService).checkPermission(eq("file"), eq(TEST_FILE_ID), eq("deleter"), eq(TEST_USERNAME));

        // Act
        boolean result = authorizationService.canDeleteFile(TEST_FILE_ID, TEST_USERNAME);

        // Assert
        assertTrue(result);
    }

    @Test
    void initializeFileOwner_Success() {
        // Arrange
        doNothing().when(authorizationService).addRelation(anyString(), anyString(), anyString(), anyString());

        // Act
        authorizationService.initializeFileOwner(TEST_FILE_ID, TEST_USERNAME);

        // Assert
        verify(authorizationService, times(4)).addRelation(anyString(), anyString(), anyString(), anyString());
    }
}
