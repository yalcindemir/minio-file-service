package com.fileservice.minioservice.exception;

import lombok.Getter;

@Getter
public class FileServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public FileServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public FileServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public static class FileNotFoundException extends FileServiceException {
        public FileNotFoundException(String message) {
            super(message, "FILE_NOT_FOUND");
        }
    }
    
    public static class FileUploadException extends FileServiceException {
        public FileUploadException(String message, Throwable cause) {
            super(message, "FILE_UPLOAD_FAILED", cause);
        }
    }
    
    public static class FileDeleteException extends FileServiceException {
        public FileDeleteException(String message, Throwable cause) {
            super(message, "FILE_DELETE_FAILED", cause);
        }
    }
    
    public static class FileAccessDeniedException extends FileServiceException {
        public FileAccessDeniedException(String message) {
            super(message, "ACCESS_DENIED");
        }
    }
    
    public static class InvalidFileTypeException extends FileServiceException {
        public InvalidFileTypeException(String message) {
            super(message, "INVALID_FILE_TYPE");
        }
    }
}
