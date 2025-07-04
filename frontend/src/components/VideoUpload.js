import React, { useState } from 'react';
import DragDropZone from './DragDropZone';
import ProgressBar from './ProgressBar';
import VideoList from './VideoList';
import videoService from '../services/videoService';
import '../styles/VideoUpload.css';

const VideoUpload = () => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatus, setUploadStatus] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [selectedVideo, setSelectedVideo] = useState(null);

  const handleFileSelect = (file) => {
    setSelectedFile(file);
    setError('');
    setSuccess('');
    setUploadProgress(0);
    setUploadStatus('');

    // Check file size and warn if it's approaching the limit
    const fileSizeMB = (file.size / (1024 * 1024)).toFixed(2);
    if (fileSizeMB > 90) {
      setError(`Warning: Your file is ${fileSizeMB}MB. The maximum allowed size is 100MB.`);
    } else if (fileSizeMB > 50) {
      setError(`Note: Your file is ${fileSizeMB}MB. Large files may take longer to upload.`);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Please select a video file first');
      return;
    }

    setUploading(true);
    setUploadStatus('uploading');
    setError('');
    setSuccess('');

    try {
      const response = await videoService.uploadVideo(
        selectedFile,
        description,
        (progressEvent) => {
          const progress = (progressEvent.loaded / progressEvent.total) * 100;
          setUploadProgress(progress);
        }
      );

      setUploadStatus('success');
      setSuccess(response.message);
      setSelectedFile(null);
      setDescription('');
      setRefreshTrigger(prev => prev + 1); // Trigger video list refresh

    } catch (err) {
      setUploadStatus('error');
      if (typeof err === 'string') {
        setError(err);
      } else if (err.error) {
        setError(err.error);
      } else if (err.message && err.message.includes('Network Error')) {
        setError('Network error occurred. Please check your internet connection and try again.');
      } else if (err.message && err.message.includes('timeout')) {
        setError('Request timed out. Your video might be too large or your connection is slow.');
      } else {
        setError('Upload failed. Please try again or use a smaller file.');
      }
      console.error('Upload error details:', err);
    } finally {
      setUploading(false);
    }
  };

  const handleClearFile = () => {
    setSelectedFile(null);
    setUploadProgress(0);
    setUploadStatus('');
    setError('');
    setSuccess('');
  };

  const handleVideoSelect = (video) => {
    setSelectedVideo(video);
  };

  const closeVideoModal = () => {
    setSelectedVideo(null);
  };

  return (
    <div className="video-upload-container">
      <div className="upload-header">
        <h1>🎯 SilentVoiceBD - Video Upload System</h1>
        <p>Upload your sign language videos for processing and translation</p>
      </div>

      {!selectedFile && !uploading && (
        <DragDropZone onFileSelect={handleFileSelect} />
      )}

      {selectedFile && !uploading && (
        <div className="selected-file">
          <div className="file-preview">
            <div className="file-icon">📹</div>
            <div className="file-details">
              <h4>{selectedFile.name}</h4>
              <p>Size: {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB</p>
              <p>Type: {selectedFile.type}</p>
            </div>
            <button className="clear-btn" onClick={handleClearFile}>❌</button>
          </div>

          <div className="description-input">
            <label htmlFor="description">Description (optional):</label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe the sign language content..."
              rows="3"
            />
          </div>

          <button
            className="upload-btn"
            onClick={handleUpload}
            disabled={uploading}
          >
            🚀 Upload Video
          </button>
        </div>
      )}

      {uploading && (
        <ProgressBar
          progress={uploadProgress}
          fileName={selectedFile?.name}
          status={uploadStatus}
        />
      )}

      {error && (
        <div className="error-message">
          ⚠️ {error}
        </div>
      )}

      {success && (
        <div className="success-message">
          ✅ {success}
        </div>
      )}

      <VideoList
        refreshTrigger={refreshTrigger}
        onVideoSelect={handleVideoSelect}
      />

      {selectedVideo && (
        <div className="video-modal" onClick={closeVideoModal}>
          <div className="video-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{selectedVideo.originalFilename}</h3>
              <button className="close-btn" onClick={closeVideoModal}>❌</button>
            </div>
            <video
              controls
              width="100%"
              height="400"
              src={videoService.getVideoStreamUrl(selectedVideo.id)}
            >
              Your browser does not support the video tag.
            </video>
          </div>
        </div>
      )}
    </div>
  );
};

export default VideoUpload;
