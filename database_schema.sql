
-- SilentVoiceBD Video Upload System Database Schema
-- PostgreSQL Database Schema for Phase 1

-- Drop existing tables if they exist (for development)
DROP TABLE IF EXISTS video_files CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Users table (basic structure for future phases)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Video files table for storing video metadata and content
CREATE TABLE video_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA, -- BLOB data for storing video content
    file_path VARCHAR(500), -- Alternative: store file system path instead of BLOB
    upload_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processing_status VARCHAR(50) DEFAULT 'UPLOADED', -- UPLOADED, PROCESSING, COMPLETED, FAILED
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    description TEXT,
    duration_seconds INTEGER,

    -- Metadata for sign language processing (future phases)
    translation_result TEXT,
    confidence_score DECIMAL(5,2),

    -- Indexes for better performance
    CONSTRAINT chk_file_size CHECK (file_size > 0),
    CONSTRAINT chk_content_type CHECK (content_type LIKE 'video/%')
);

-- Create indexes for better query performance
CREATE INDEX idx_video_files_user_id ON video_files(user_id);
CREATE INDEX idx_video_files_upload_timestamp ON video_files(upload_timestamp);
CREATE INDEX idx_video_files_processing_status ON video_files(processing_status);
CREATE INDEX idx_video_files_content_type ON video_files(content_type);

-- Insert sample user for testing (password is 'testpassword' hashed)
INSERT INTO users (username, email, password_hash) VALUES 
('testuser', 'test@silentvoicebd.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye');

-- Sample data for testing
INSERT INTO video_files (filename, original_filename, content_type, file_size, processing_status, description) VALUES 
('sample_video_001.mp4', 'hello_sign.mp4', 'video/mp4', 1048576, 'UPLOADED', 'Sample sign language video for testing');
