package com.fileservice.minioservice.repository;

import com.fileservice.minioservice.model.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
    
    Optional<ShareLink> findByToken(String token);
    
    List<ShareLink> findByFileId(Long fileId);
    
    List<ShareLink> findByCreatedBy(String username);
    
    @Query("SELECT s FROM ShareLink s WHERE s.expiresAt < :now")
    List<ShareLink> findExpiredLinks(@Param("now") LocalDateTime now);
    
    void deleteByToken(String token);
}
