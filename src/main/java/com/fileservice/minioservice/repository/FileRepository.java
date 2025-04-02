package com.fileservice.minioservice.repository;

import com.fileservice.minioservice.model.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    
    List<FileEntity> findByOwner(String owner);
    
    Optional<FileEntity> findByObjectNameAndBucketName(String objectName, String bucketName);
    
    @Query("SELECT f FROM FileEntity f WHERE f.expiresAt < :now")
    List<FileEntity> findExpiredFiles(@Param("now") LocalDateTime now);
    
    List<FileEntity> findByContentTypeStartingWith(String contentTypePrefix);
    
    List<FileEntity> findByFilenameContainingIgnoreCase(String filenamePattern);
}
