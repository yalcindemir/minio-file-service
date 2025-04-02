package com.fileservice.minioservice.repository;

import com.fileservice.minioservice.model.VirusScanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VirusScanRepository extends JpaRepository<VirusScanEntity, Long> {
    
    Optional<VirusScanEntity> findByFileId(String fileId);
    
    void deleteByFileId(String fileId);
}
