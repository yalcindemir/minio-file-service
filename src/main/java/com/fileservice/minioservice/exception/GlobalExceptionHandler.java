package com.fileservice.minioservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(FileServiceException.FileNotFoundException.class)
    public ResponseEntity<Object> handleFileNotFoundException(FileServiceException.FileNotFoundException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), ex.getErrorCode(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FileServiceException.FileUploadException.class)
    public ResponseEntity<Object> handleFileUploadException(FileServiceException.FileUploadException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), ex.getErrorCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(FileServiceException.FileDeleteException.class)
    public ResponseEntity<Object> handleFileDeleteException(FileServiceException.FileDeleteException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), ex.getErrorCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(FileServiceException.FileAccessDeniedException.class)
    public ResponseEntity<Object> handleFileAccessDeniedException(FileServiceException.FileAccessDeniedException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), ex.getErrorCode(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(FileServiceException.InvalidFileTypeException.class)
    public ResponseEntity<Object> handleInvalidFileTypeException(FileServiceException.InvalidFileTypeException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), ex.getErrorCode(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, WebRequest request) {
        return buildErrorResponse("File size exceeds the maximum allowed size", "MAX_SIZE_EXCEEDED", HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), "INVALID_ARGUMENT", HttpStatus.BAD_REQUEST);
    }

    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, 
                                                                 org.springframework.http.HttpHeaders headers,
                                                                 HttpStatus status, 
                                                                 WebRequest request) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage(),
                        (existing, replacement) -> existing + "; " + replacement
                ));
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", "Validation failed");
        body.put("errorCode", "VALIDATION_FAILED");
        body.put("errors", errors);
        
        return new ResponseEntity<>(body, headers, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        return buildErrorResponse("An unexpected error occurred: " + ex.getMessage(), "INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Object> buildErrorResponse(String message, String errorCode, HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("errorCode", errorCode);
        
        return new ResponseEntity<>(body, status);
    }
}
