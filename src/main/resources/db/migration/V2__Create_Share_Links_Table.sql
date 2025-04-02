CREATE TABLE share_links (
    id SERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    permission VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT fk_share_links_file FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE
);

CREATE INDEX idx_share_links_token ON share_links (token);
CREATE INDEX idx_share_links_file_id ON share_links (file_id);
CREATE INDEX idx_share_links_created_by ON share_links (created_by);
CREATE INDEX idx_share_links_expires_at ON share_links (expires_at);
