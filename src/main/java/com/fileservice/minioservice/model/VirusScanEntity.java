package com.fileservice.minioservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "virus_scan_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirusScanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileId;

    @Column(nullable = false)
    private boolean scanned;

    @Column(nullable = false)
    private boolean clean;

    @Column
    private Integer positives;

    @Column
    private Integer total;

    @Column
    private String scanId;

    @Column
    private String resource;

    @Column
    private String permalink;

    @Column(length = 1000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime scanDate;

    @PrePersist
    protected void onCreate() {
        scanDate = LocalDateTime.now();
    }
}
