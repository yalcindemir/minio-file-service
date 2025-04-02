CREATE TABLE files (
    id SERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    path VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    bucket_name VARCHAR(100) NOT NULL,
    object_name VARCHAR(255) NOT NULL,
    owner VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE TABLE file_thumbnails (
    file_id BIGINT NOT NULL,
    thumbnail_path VARCHAR(255) NOT NULL,
    PRIMARY KEY (file_id, thumbnail_path),
    CONSTRAINT fk_file_thumbnails_file FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE
);

CREATE INDEX idx_files_owner ON files (owner);
CREATE INDEX idx_files_content_type ON files (content_type);
CREATE INDEX idx_files_expires_at ON files (expires_at);
CREATE INDEX idx_files_object_name ON files (object_name, bucket_name);
