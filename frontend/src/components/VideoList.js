import React, { useState, useEffect } from 'react';
import videoService from '../services/videoService';
import { useAuth } from '../contexts/AuthContext';

const VideoList = ({ refreshTrigger, onVideoSelect }) => {
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { user } = useAuth();

  // Single function to load videos
  const loadVideos = async () => {
    if (!user) return; // Don't load if no user authenticated

    setLoading(true);
    setError('');

    try {
      const response = await videoService.getAllVideos();
      // Handle different response formats from backend
      const videoList = response.videos || response || [];
      setVideos(Array.isArray(videoList) ? videoList : []);
    } catch (err) {
      console.error('Error loading videos:', err);
      setError(err.message || 'Failed to load videos. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Load videos when component mounts or dependencies change
  useEffect(() => {
    loadVideos();
  }, [user, refreshTrigger]);

  // Handle video deletion
  const handleDelete = async (videoId, videoName) => {
    if (!window.confirm(`Are you sure you want to delete "${videoName || 'this video'}"?`)) {
      return;
    }

    try {
      await videoService.deleteVideo(videoId);
      // Remove deleted video from state
      setVideos(prevVideos => prevVideos.filter(video => video.id !== videoId));

      // Clear any existing errors
      setError('');
    } catch (err) {
      console.error('Error deleting video:', err);
      setError(err.message || 'Failed to delete video. Please try again.');
    }
  };

  // Utility functions
  const formatFileSize = (bytes) => {
    if (!bytes || bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown';
    try {
      return new Date(dateString).toLocaleString();
    } catch (error) {
      return 'Invalid date';
    }
  };

  const getStatusClassName = (status) => {
    if (!status) return 'unknown';
    return status.toLowerCase().replace(/[^a-z]/g, '-');
  };

  // Render loading state
  if (loading) {
    return (
      <div className="video-list-container">
        <div className="loading">
          <div className="loading-spinner"></div>
          <p>Loading your videos...</p>
        </div>
      </div>
    );
  }

  // Render error state
  if (error) {
    return (
      <div className="video-list-container">
        <div className="error-message">
          <p>⚠️ {error}</p>
          <button onClick={loadVideos} className="retry-btn">
            Try Again
          </button>
        </div>
      </div>
    );
  }

  // Render empty state
  if (videos.length === 0) {
    return (
      <div className="video-list-container">
        <div className="empty-state">
          <div className="empty-icon">📹</div>
          <h3>No Videos Yet</h3>
          <p>Upload your first sign language video to get started!</p>
        </div>
      </div>
    );
  }

  // Render video list
  return (
    <div className="video-list-container">
      <div className="video-list-header">
        <h3>Your Videos ({videos.length})</h3>
        <button onClick={loadVideos} className="refresh-btn" title="Refresh list">
          🔄
        </button>
      </div>

      <div className="video-grid">
        {videos.map((video) => (
          <div key={video.id} className="video-card">
            <div className="video-header">
              <h4 className="video-title" title={video.originalFilename || video.filename}>
                {video.originalFilename || video.filename || 'Unnamed Video'}
              </h4>
              <div className="video-actions">
                <button
                  className="btn-action btn-view"
                  onClick={() => onVideoSelect && onVideoSelect(video)}
                  title="View video"
                  aria-label={`View ${video.originalFilename || 'video'}`}
                >
                  👁️
                </button>
                <a
                  href={videoService.getVideoDownloadUrl(video.id)}
                  className="btn-action btn-download"
                  title="Download video"
                  download={video.originalFilename || video.filename}
                  aria-label={`Download ${video.originalFilename || 'video'}`}
                >
                  ⬇️
                </a>
                <button
                  className="btn-action btn-delete"
                  onClick={() => handleDelete(video.id, video.originalFilename || video.filename)}
                  title="Delete video"
                  aria-label={`Delete ${video.originalFilename || 'video'}`}
                >
                  🗑️
                </button>
              </div>
            </div>

            <div className="video-details">
              <div className="detail-row">
                <span className="label">Size:</span>
                <span className="value">{formatFileSize(video.fileSize)}</span>
              </div>

              <div className="detail-row">
                <span className="label">Type:</span>
                <span className="value">{video.contentType || 'Unknown'}</span>
              </div>

              <div className="detail-row">
                <span className="label">Uploaded:</span>
                <span className="value">{formatDate(video.uploadTimestamp)}</span>
              </div>

              <div className="detail-row">
                <span className="label">Status:</span>
                <span className={`status ${getStatusClassName(video.processingStatus)}`}>
                  {video.processingStatus || 'Unknown'}
                </span>
              </div>

              {video.durationSeconds && (
                <div className="detail-row">
                  <span className="label">Duration:</span>
                  <span className="value">{video.durationSeconds}s</span>
                </div>
              )}

              {video.description && (
                <div className="detail-row description">
                  <span className="label">Description:</span>
                  <span className="value">{video.description}</span>
                </div>
              )}
            </div>

            {/* Processing indicator */}
            {video.processingStatus === 'PROCESSING' && (
              <div className="processing-indicator">
                <div className="processing-bar">
                  <div className="processing-progress"></div>
                </div>
                <span className="processing-text">Processing video...</span>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default VideoList;
