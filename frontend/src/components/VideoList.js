import React, { useState, useEffect } from 'react';
import videoService from '../services/videoService';

const VideoList = ({ refreshTrigger, onVideoSelect }) => {
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadVideos();
  }, [refreshTrigger]);

  const loadVideos = async () => {
    setLoading(true);
    setError('');

    try {
      const videoList = await videoService.getAllVideos();
      setVideos(videoList);
    } catch (err) {
      setError('Failed to load videos');
      console.error('Error loading videos:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (videoId, fileName) => {
    if (window.confirm(`Are you sure you want to delete "${fileName}"?`)) {
      try {
        await videoService.deleteVideo(videoId);
        setVideos(videos.filter(video => video.id !== videoId));
      } catch (err) {
        setError('Failed to delete video');
        console.error('Error deleting video:', err);
      }
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  if (loading) {
    return <div className="loading">Loading videos...</div>;
  }

  return (
    <div className="video-list">
      <h3>Uploaded Videos ({videos.length})</h3>

      {error && (
        <div className="error-message">
          ⚠️ {error}
        </div>
      )}

      {videos.length === 0 ? (
        <div className="empty-state">
          <p>No videos uploaded yet. Upload your first sign language video to get started!</p>
        </div>
      ) : (
        <div className="video-grid">
          {videos.map((video) => (
            <div key={video.id} className="video-card">
              <div className="video-header">
                <h4 className="video-title">{video.originalFilename}</h4>
                <div className="video-actions">
                  <button
                    className="btn-stream"
                    onClick={() => onVideoSelect && onVideoSelect(video)}
                    title="View video"
                  >
                    👁️
                  </button>
                  <a
                    href={videoService.getVideoDownloadUrl(video.id)}
                    className="btn-download"
                    title="Download video"
                    download
                  >
                    ⬇️
                  </a>
                  <button
                    className="btn-delete"
                    onClick={() => handleDelete(video.id, video.originalFilename)}
                    title="Delete video"
                  >
                    🗑️
                  </button>
                </div>
              </div>

              <div className="video-details">
                <div className="detail-row">
                  <span className="label">Size:</span>
                  <span>{formatFileSize(video.fileSize)}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Type:</span>
                  <span>{video.contentType}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Uploaded:</span>
                  <span>{formatDate(video.uploadTimestamp)}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Status:</span>
                  <span className={`status ${video.processingStatus.toLowerCase()}`}>
                    {video.processingStatus}
                  </span>
                </div>
                {video.description && (
                  <div className="detail-row">
                    <span className="label">Description:</span>
                    <span>{video.description}</span>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default VideoList;
