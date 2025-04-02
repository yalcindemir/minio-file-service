package com.fileservice.minioservice.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DatabaseConfig {

    /**
     * Custom Flyway migration strategy for development environment
     * This will clean the database and run migrations from scratch when in dev profile
     */
    @Bean
    @Profile("dev")
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            log.info("Cleaning database before migration in dev environment");
            flyway.clean();
            flyway.migrate();
        };
    }
    
    /**
     * Standard migration strategy for production environment
     * This will only run migrations without cleaning the database
     */
    @Bean
    @Profile("!dev")
    public FlywayMigrationStrategy migrateStrategy() {
        return flyway -> {
            log.info("Running database migrations");
            flyway.migrate();
        };
    }
}
