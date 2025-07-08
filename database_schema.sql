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
    file_data BYTEA,
    -- BLOB data for storing video content
    file_path VARCHAR(500),
    -- Alternative: store file system path instead of BLOB
    upload_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processing_status VARCHAR(50) DEFAULT 'UPLOADED',
    -- UPLOADED, PROCESSING, COMPLETED, FAILED
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    description TEXT,
    duration_seconds INTEGER,
    -- Metadata for sign language processing (future phases)
    translation_result TEXT,
    confidence_score DECIMAL(5, 2),
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
INSERT INTO users (username, email, password_hash)
VALUES (
        'testuser',
        'test@silentvoicebd.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMye'
    );
-- Sample data for testing
INSERT INTO video_files (
        filename,
        original_filename,
        content_type,
        file_size,
        processing_status,
        description
    )
VALUES (
        'sample_video_001.mp4',
        'hello_sign.mp4',
        'video/mp4',
        1048576,
        'UPLOADED',
        'Sample sign language video for testing'
    );
-- AI predictions table
CREATE TABLE sign_language_predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_file_id UUID NOT NULL REFERENCES video_files(id) ON DELETE CASCADE,
    predicted_text TEXT NOT NULL,
    confidence_score DECIMAL(5, 4) NOT NULL CHECK (
        confidence_score >= 0
        AND confidence_score <= 1
    ),
    processing_time_ms INTEGER,
    model_version VARCHAR(50) DEFAULT 'bangla_lstm_v1',
    prediction_metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Indexes for performance
CREATE INDEX idx_sign_predictions_video_id ON sign_language_predictions(video_file_id);
CREATE INDEX idx_sign_predictions_confidence ON sign_language_predictions(confidence_score DESC);
CREATE INDEX idx_sign_predictions_created_at ON sign_language_predictions(created_at DESC);
-- Update trigger for timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column() RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';
CREATE TRIGGER update_sign_predictions_updated_at BEFORE
UPDATE ON sign_language_predictions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();