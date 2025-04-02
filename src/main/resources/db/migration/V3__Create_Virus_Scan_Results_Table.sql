CREATE TABLE virus_scan_results (
    id SERIAL PRIMARY KEY,
    file_id VARCHAR(255) NOT NULL,
    scanned BOOLEAN NOT NULL,
    clean BOOLEAN NOT NULL,
    positives INTEGER,
    total INTEGER,
    scan_id VARCHAR(255),
    resource VARCHAR(255),
    permalink VARCHAR(255),
    message VARCHAR(1000),
    scan_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_virus_scan_file FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE
);

CREATE INDEX idx_virus_scan_file_id ON virus_scan_results (file_id);
