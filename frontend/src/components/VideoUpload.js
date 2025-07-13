import React, { useState, useRef } from 'react';
import DragDropZone from './DragDropZone';
import ProgressBar from './ProgressBar';
import VideoList from './VideoList';
import PredictionDisplay from './PredictionDisplay';
import videoService from '../services/videoService';
import '../styles/VideoUpload.css';

const VideoUpload = () => {
  // Existing state
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatus, setUploadStatus] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [selectedVideo, setSelectedVideo] = useState(null);

  // AI Integration state
  const [videoId, setVideoId] = useState(null);
  const [aiProcessing, setAiProcessing] = useState(false);
  const [aiProgress, setAiProgress] = useState(0);
  const [prediction, setPrediction] = useState(null);
  const [enableAI, setEnableAI] = useState(true);
  const [processingPhase, setProcessingPhase] = useState('');

  const pollIntervalRef = useRef(null);

  // ✅ FIXED: Debug function properly placed at component level
  const debugAPIResponse = async (videoId) => {
    try {
      console.log('=== DEBUG: Testing API endpoints ===');
      console.log('Video ID:', videoId);

      // Test status endpoint
      const statusUrl = `/api/videos/${videoId}/status`;
      console.log('Testing URL:', statusUrl);

      const statusResponse = await fetch(statusUrl);
      console.log('Status endpoint response status:', statusResponse.status);
      console.log('Status endpoint content-type:', statusResponse.headers.get('content-type'));

      if (statusResponse.ok) {
        const contentType = statusResponse.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
          const statusData = await statusResponse.json();
          console.log('Status data:', statusData);
        } else {
          const responseText = await statusResponse.text();
          console.log('Non-JSON response:', responseText.substring(0, 500));
        }
      }

      // Test prediction endpoint
      const predictionUrl = `/api/ai/predictions/${videoId}/latest`;
      console.log('Testing prediction URL:', predictionUrl);

      const predictionResponse = await fetch(predictionUrl);
      console.log('Prediction endpoint response status:', predictionResponse.status);

      if (predictionResponse.ok) {
        const predictionData = await predictionResponse.json();
        console.log('Prediction data:', predictionData);
      } else {
        console.log('Prediction not ready yet or error occurred');
      }

      console.log('=== DEBUG: End API test ===');
    } catch (error) {
      console.error('Debug API test error:', error);
    }
  };

  const handleFileSelect = (file) => {
    setSelectedFile(file);
    setError('');
    setSuccess('');
    setUploadProgress(0);
    setUploadStatus('');

    resetAIState();

    const fileSizeMB = (file.size / (1024 * 1024)).toFixed(2);
    if (fileSizeMB > 90) {
      setError(`Warning: Your file is ${fileSizeMB}MB. The maximum allowed size is 100MB.`);
    } else if (fileSizeMB > 50) {
      setError(`Note: Your file is ${fileSizeMB}MB. Large files may take longer to upload and process.`);
    }
  };

  const resetAIState = () => {
    setVideoId(null);
    setAiProcessing(false);
    setAiProgress(0);
    setPrediction(null);
    setProcessingPhase('');
    if (pollIntervalRef.current) {
      clearInterval(pollIntervalRef.current);
      pollIntervalRef.current = null;
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Please select a video file first');
      return;
    }

    setUploading(true);
    setUploadStatus('uploading');
    setProcessingPhase('uploading');
    setError('');
    setSuccess('');

    try {
      const response = await videoService.uploadVideo(
        selectedFile,
        description,
        enableAI,
        (progressData) => {
          setUploadProgress(progressData.progress);
        }
      );

      setUploadStatus('success');
      setVideoId(response.id);

      // ✅ FIXED: Debug function called after successful upload
      debugAPIResponse(response.id);

      if (enableAI) {
        setSuccess('Video uploaded successfully! AI analysis starting...');
        setProcessingPhase('extracting');
        startAIProcessingPoll(response.id);
      } else {
        setSuccess(response.message);
        setProcessingPhase('completed');
      }

      setSelectedFile(null);
      setDescription('');
      setRefreshTrigger(prev => prev + 1);

    } catch (err) {
      setUploadStatus('error');
      setProcessingPhase('');
      handleUploadError(err);
    } finally {
      setUploading(false);
    }
  };

  const handleUploadError = (err) => {
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
  };

  const startAIProcessingPoll = (videoId) => {
    setAiProcessing(true);
    setAiProgress(10);

    setTimeout(() => {
      setProcessingPhase('analyzing');
      setAiProgress(25);
      pollForPrediction(videoId);
    }, 8000);
  };

  const pollForPrediction = (videoId) => {
    const maxAttempts = 45;
    let attempts = 0;

    const poll = async () => {
      try {
        attempts++;
        console.log(`Polling attempt ${attempts}/${maxAttempts} for video ${videoId}`);

        const statusResponse = await fetch(`/api/videos/${videoId}/status`);
        console.log('Status response status:', statusResponse.status);
        console.log('Status response headers:', statusResponse.headers.get('content-type'));

        if (statusResponse.ok) {
          const contentType = statusResponse.headers.get('content-type');
          if (contentType && contentType.includes('application/json')) {
            const status = await statusResponse.json();
            console.log('Status data received:', status);

            if (status.success !== false) {
              if (status.aiComplete === true && status.prediction) {
                console.log('AI processing complete! Prediction:', status.prediction);

                const predictionData = {
                  id: status.predictionId,
                  predictedText: status.prediction,
                  confidenceScore: status.confidence,
                  modelVersion: status.modelVersion,
                  processingTimeMs: status.processingTime,
                  createdAt: new Date().toISOString()
                };

                setPrediction(predictionData);
                setAiProcessing(false);
                setAiProgress(100);
                setProcessingPhase('completed');
                setSuccess('🎉 AI analysis completed! Sign language recognized.');

                if (pollIntervalRef.current) {
                  clearInterval(pollIntervalRef.current);
                  pollIntervalRef.current = null;
                }
                return;
              } else if (status.aiComplete === false) {
                console.log('AI still processing...', status.status);
              }
            } else {
              console.error('Status response indicates error:', status.error);
            }
          } else {
            console.error('Response is not JSON, received:', contentType);
            const responseText = await statusResponse.text();
            console.error('Response body:', responseText.substring(0, 200));
          }
        } else {
          console.error('Status check failed with status:', statusResponse.status);
          const responseText = await statusResponse.text();
          console.error('Error response:', responseText.substring(0, 200));
        }

        const progress = Math.min(25 + (attempts / maxAttempts) * 65, 90);
        setAiProgress(progress);

        if (attempts >= maxAttempts) {
          console.error('Polling timeout reached');
          setAiProcessing(false);
          setError('AI processing timeout. Backend completed but frontend cannot retrieve results.');
          setProcessingPhase('');

          if (pollIntervalRef.current) {
            clearInterval(pollIntervalRef.current);
            pollIntervalRef.current = null;
          }
        }
      } catch (err) {
        console.error('Polling error:', err);
        attempts++;

        if (attempts >= maxAttempts) {
          setAiProcessing(false);
          setError('Network error while checking AI processing status.');
          setProcessingPhase('');

          if (pollIntervalRef.current) {
            clearInterval(pollIntervalRef.current);
            pollIntervalRef.current = null;
          }
        }
      }
    };

    poll();
    pollIntervalRef.current = setInterval(poll, 3000);
  };

  const handleClearFile = () => {
    setSelectedFile(null);
    setUploadProgress(0);
    setUploadStatus('');
    setError('');
    setSuccess('');
    resetAIState();
  };

  const handleVideoSelect = (video) => {
    setSelectedVideo(video);
  };

  const closeVideoModal = () => {
    setSelectedVideo(null);
  };

  const resetUpload = () => {
    handleClearFile();
    setPrediction(null);
    setProcessingPhase('');
  };

  const checkVideoAIPrediction = async (videoId) => {
    try {
      const response = await fetch(`/api/ai/predictions/${videoId}/latest`);
      if (response.ok) {
        const predictionData = await response.json();
        if (predictionData) {
          alert(`AI Prediction: ${predictionData.predictedText} (${(predictionData.confidenceScore * 100).toFixed(1)}% confidence)`);
        } else {
          alert('No AI prediction found for this video.');
        }
      } else {
        alert('No AI analysis available for this video.');
      }
    } catch (error) {
      console.error('Error checking AI prediction:', error);
      alert('Error checking AI prediction.');
    }
  };

  const getProcessingMessage = () => {
    switch (processingPhase) {
      case 'uploading':
        return 'Uploading video to server...';
      case 'extracting':
        return 'Extracting frames from video...';
      case 'analyzing':
        return 'AI analyzing sign language patterns...';
      case 'completed':
        return prediction ? 'Analysis complete!' : 'Upload complete!';
      default:
        return 'Ready to upload';
    }
  };

  // ✅ FIXED: Clean useEffect without debug function
  React.useEffect(() => {
    return () => {
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
      }
    };
  }, []);

  return (
    <div className="video-upload-container">
      <div className="upload-header">
        <h1>🎯 SilentVoice_BD - Bangla Sign Language Recognition</h1>
        <p>Upload your sign language videos for AI-powered translation</p>
      </div>

      {!uploading && !aiProcessing && !prediction && (
        <div className="ai-toggle">
          <label className="toggle-container">
            <input
              type="checkbox"
              checked={enableAI}
              onChange={(e) => setEnableAI(e.target.checked)}
            />
            <span className="toggle-slider"></span>
            <span className="toggle-label">
              {enableAI ? '🤖 AI Analysis Enabled' : '📹 Upload Only'}
            </span>
          </label>
          <p className="toggle-description">
            {enableAI
              ? 'Video will be analyzed for sign language recognition'
              : 'Video will be uploaded without AI analysis'
            }
          </p>
        </div>
      )}

      {!selectedFile && !uploading && !aiProcessing && !prediction && (
        <DragDropZone onFileSelect={handleFileSelect} />
      )}

      {selectedFile && !uploading && !aiProcessing && (
        <div className="selected-file">
          <div className="file-preview">
            <div className="file-icon">📹</div>
            <div className="file-details">
              <h4>{selectedFile.name}</h4>
              <p>Size: {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB</p>
              <p>Type: {selectedFile.type}</p>
              {enableAI && (
                <p className="ai-notice">🤖 AI analysis will be performed</p>
              )}
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
            {enableAI ? '🚀 Upload & Analyze' : '📤 Upload Video'}
          </button>
        </div>
      )}

      {(uploading || aiProcessing) && (
        <div className="processing-section">
          <ProgressBar
            progress={uploading ? uploadProgress : aiProgress}
            fileName={selectedFile?.name}
            status={uploadStatus}
          />
          <div className="processing-status">
            <h3>{getProcessingMessage()}</h3>
            {aiProcessing && (
              <div className="ai-processing-indicator">
                <div className="ai-spinner">🤖</div>
                <p>AI is analyzing your sign language video...</p>
                <p className="processing-time">This may take 30-90 seconds</p>
              </div>
            )}
          </div>
        </div>
      )}

      {error && (
        <div className="error-message">
          ⚠️ {error}
          {(error.includes('timeout') || error.includes('processing')) && (
            <button className="retry-btn" onClick={resetUpload}>
              🔄 Try Again
            </button>
          )}
        </div>
      )}

      {success && (
        <div className="success-message">
          ✅ {success}
        </div>
      )}

      {prediction && (
        <PredictionDisplay
          prediction={prediction}
          uploadStatus="completed"
          onReset={resetUpload}
          videoId={videoId}
        />
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

            <div className="modal-ai-info">
              <button
                className="check-ai-btn"
                onClick={() => checkVideoAIPrediction(selectedVideo.id)}
              >
                🤖 Check AI Analysis
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default VideoUpload;

