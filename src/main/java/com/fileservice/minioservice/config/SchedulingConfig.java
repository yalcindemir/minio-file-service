package com.fileservice.minioservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fileservice.minioservice.service.MinioService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    
    @Component
    @RequiredArgsConstructor
    @Slf4j
    public static class ScheduledTasks {
        
        private final MinioService minioService;
        
        @PostConstruct
        public void initialize() {
            minioService.initialize();
            log.info("MinIO service initialized");
        }
        
        @Scheduled(cron = "0 0 1 * * ?") // Run at 1 AM every day
        public void cleanupExpiredFiles() {
            log.info("Starting scheduled cleanup of expired files");
            minioService.cleanupExpiredFiles();
            log.info("Completed scheduled cleanup of expired files");
        }
    }
}
