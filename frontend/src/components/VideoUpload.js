import React, { useState, useRef, useEffect } from 'react';
import DragDropZone from './DragDropZone';
import ProgressBar from './ProgressBar';
import VideoList from './VideoList';
import PredictionDisplay from './PredictionDisplay';
import videoService from '../services/videoService';
import '../styles/VideoUpload.css';

const VideoUpload = () => {
  // File upload state
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatus, setUploadStatus] = useState('');
  const [description, setDescription] = useState('');

  // UI state
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [selectedVideo, setSelectedVideo] = useState(null);

  // AI processing state
  const [videoId, setVideoId] = useState(null);
  const [aiProcessing, setAiProcessing] = useState(false);
  const [aiProgress, setAiProgress] = useState(0);
  const [prediction, setPrediction] = useState(null);
  const [enableAI, setEnableAI] = useState(true);
  const [processingPhase, setProcessingPhase] = useState('');

  // Refs
  const pollIntervalRef = useRef(null);
  const pollTimeoutRef = useRef(null);

  // File selection handler
  const handleFileSelect = (file) => {
    setSelectedFile(file);
    setError('');
    setSuccess('');
    setUploadProgress(0);
    setUploadStatus('');
    resetAIState();

    // File size validation
    const fileSizeMB = (file.size / (1024 * 1024)).toFixed(2);
    if (fileSizeMB > 100) {
      setError(`File too large: ${fileSizeMB}MB. Maximum allowed size is 100MB.`);
      return;
    }
    if (fileSizeMB > 50) {
      setError(`Large file detected: ${fileSizeMB}MB. Upload may take longer.`);
    }
  };

  // Reset AI-related state
  const resetAIState = () => {
    setVideoId(null);
    setAiProcessing(false);
    setAiProgress(0);
    setPrediction(null);
    setProcessingPhase('');

    // Clear all timers
    if (pollIntervalRef.current) {
      clearInterval(pollIntervalRef.current);
      pollIntervalRef.current = null;
    }
    if (pollTimeoutRef.current) {
      clearTimeout(pollTimeoutRef.current);
      pollTimeoutRef.current = null;
    }
  };

  // Main upload handler
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
    setUploadProgress(0);

    try {
      console.log('🚀 Starting upload...');
      const response = await videoService.uploadVideo(
        selectedFile,
        description,
        enableAI,
        (progressData) => {
          setUploadProgress(progressData.progress);
        }
      );

      console.log('✅ Upload successful:', response);
      setUploadStatus('success');
      setVideoId(response.id);
      setUploadProgress(100);

      if (enableAI && response.id) {
        setSuccess('Video uploaded successfully! Starting AI analysis...');
        setProcessingPhase('processing');
        startAIProcessing(response.id);
      } else {
        setSuccess('Video uploaded successfully!');
        setProcessingPhase('completed');
      }

      // Reset form
      setSelectedFile(null);
      setDescription('');
      setRefreshTrigger(prev => prev + 1);

    } catch (err) {
      console.error('❌ Upload failed:', err);
      setUploadStatus('error');
      setProcessingPhase('');
      handleUploadError(err);
    } finally {
      setUploading(false);
    }
  };

  // Handle upload errors
  const handleUploadError = (err) => {
    let errorMessage = 'Upload failed. Please try again.';

    if (typeof err === 'string') {
      errorMessage = err;
    } else if (err?.message) {
      if (err.message.includes('Network')) {
        errorMessage = 'Network error. Please check your connection and try again.';
      } else if (err.message.includes('timeout')) {
        errorMessage = 'Upload timeout. Please try with a smaller file.';
      } else if (err.message.includes('401')) {
        errorMessage = 'Authentication error. Please refresh and login again.';
      } else {
        errorMessage = err.message;
      }
    }

    setError(errorMessage);
  };

  // Start AI processing with proper polling
  const startAIProcessing = (videoId) => {
    console.log('🤖 Starting AI processing for video:', videoId);
    setAiProcessing(true);
    setAiProgress(10);

    // Start polling after a short delay to allow backend processing to begin
    pollTimeoutRef.current = setTimeout(() => {
      setAiProgress(25);
      setProcessingPhase('analyzing');
      startPolling(videoId);
    }, 3000);
  };

  // Fixed polling function with better response handling
  const startPolling = (videoId) => {
    let attempts = 0;
    const maxAttempts = 45;

    const poll = async () => {
      attempts++;
      console.log(`📡 Polling attempt ${attempts}/${maxAttempts} for video ${videoId}`);

      try {
        // Check video status
        const statusResponse = await videoService.getVideoStatus(videoId);
        console.log('📊 Full status response:', statusResponse);

        // Update progress
        const progressPercent = Math.min(25 + (attempts / maxAttempts) * 65, 95);
        setAiProgress(progressPercent);

        // Check multiple possible completion indicators
        const isCompleted = statusResponse.processingStatus === 'COMPLETED' ||
                           statusResponse.aiComplete === true ||
                           statusResponse.status === 'COMPLETED';

        const isFailed = statusResponse.processingStatus === 'FAILED' ||
                         statusResponse.status === 'FAILED' ||
                         statusResponse.success === false;

        if (isCompleted) {
          console.log('✅ Processing completed, extracting prediction data...');

          let predictionData = null;

          // Try to extract prediction from status response first
          if (statusResponse.prediction || statusResponse.predictedText) {
            console.log('🎯 Prediction found in status response');
            predictionData = {
              id: statusResponse.predictionId || `pred_${videoId}_${Date.now()}`,
              predictedText: statusResponse.prediction || statusResponse.predictedText,
              confidenceScore: statusResponse.confidence || statusResponse.confidenceScore || 0.95,
              modelVersion: statusResponse.modelVersion || 'bangla_lstm_v1',
              processingTimeMs: statusResponse.processingTime || statusResponse.processingTimeMs,
              createdAt: new Date().toISOString()
            };
          } else {
            // Fallback: try to get prediction from dedicated endpoint
            console.log('🔍 No prediction in status, trying dedicated endpoint...');
            try {
              const prediction = await videoService.getLatestPrediction(videoId);
              if (prediction) {
                predictionData = {
                  id: prediction.id,
                  predictedText: prediction.predictedText,
                  confidenceScore: prediction.confidenceScore,
                  modelVersion: prediction.modelVersion || 'Unknown',
                  processingTimeMs: prediction.processingTimeMs,
                  createdAt: prediction.createdAt
                };
              }
            } catch (predError) {
              console.warn('⚠️ Could not fetch from prediction endpoint:', predError);
            }
          }

          if (predictionData) {
            console.log('🎉 Setting prediction data:', predictionData);

            setPrediction(predictionData);
            setAiProcessing(false);
            setAiProgress(100);
            setProcessingPhase('completed');
            setSuccess(`🎉 AI analysis completed! Recognized: "${predictionData.predictedText}"`);

            stopPolling();
            return;
          } else {
            console.warn('⚠️ Processing marked complete but no prediction data found');
            // Continue polling a bit more in case prediction is delayed
            if (attempts > maxAttempts - 10) {
              setError('AI processing completed but prediction data is not available yet.');
              setAiProcessing(false);
              setProcessingPhase('');
              stopPolling();
              return;
            }
          }
        } else if (isFailed) {
          console.error('❌ Processing failed:', statusResponse);
          setError('AI processing failed. Please try again.');
          setAiProcessing(false);
          setProcessingPhase('');
          stopPolling();
          return;
        } else {
          console.log('⏳ Still processing...', {
            processingStatus: statusResponse.processingStatus,
            aiComplete: statusResponse.aiComplete,
            status: statusResponse.status
          });
        }

        // Check if we've reached max attempts
        if (attempts >= maxAttempts) {
          console.warn('⚠️ Polling timeout reached');
          setError('AI processing is taking longer than expected. The analysis may have completed - please check your video list.');
          setAiProcessing(false);
          setProcessingPhase('');
          stopPolling();
        }

      } catch (error) {
        console.error(`❌ Polling error on attempt ${attempts}:`, error);

        // Only stop if we've reached max attempts or it's an auth error
        if (attempts >= maxAttempts || error.message?.includes('401')) {
          setError('Unable to check AI processing status. Please refresh and check your videos.');
          setAiProcessing(false);
          setProcessingPhase('');
          stopPolling();
        }
        // Continue polling for other errors (temporary network issues)
      }
    };

    // Start polling immediately, then every 3 seconds
    poll();
    pollIntervalRef.current = setInterval(poll, 3000);
  };

  // Stop polling function
  const stopPolling = () => {
    if (pollIntervalRef.current) {
      clearInterval(pollIntervalRef.current);
      pollIntervalRef.current = null;
    }
    if (pollTimeoutRef.current) {
      clearTimeout(pollTimeoutRef.current);
      pollTimeoutRef.current = null;
    }
  };

  // Clear file selection
  const handleClearFile = () => {
    setSelectedFile(null);
    setUploadProgress(0);
    setUploadStatus('');
    setError('');
    setSuccess('');
    resetAIState();
  };

  // Video selection for modal
  const handleVideoSelect = (video) => {
    setSelectedVideo(video);
  };

  const closeVideoModal = () => {
    setSelectedVideo(null);
  };

  // Reset everything
  const resetUpload = () => {
    handleClearFile();
    setPrediction(null);
    setProcessingPhase('');
  };

  // Check AI prediction for existing video
  const checkVideoAIPrediction = async (videoId) => {
    try {
      console.log('🔍 Checking AI prediction for video:', videoId);
      const predictionData = await videoService.getLatestPrediction(videoId);

      if (predictionData) {
        const confidence = (predictionData.confidenceScore * 100).toFixed(1);
        alert(`🤖 AI Prediction: "${predictionData.predictedText}"\n📊 Confidence: ${confidence}%`);
      } else {
        alert('No AI prediction found for this video yet.');
      }
    } catch (error) {
      console.error('Error checking AI prediction:', error);
      if (error.message.includes('404')) {
        alert('No AI analysis available for this video.');
      } else {
        alert('Error checking AI prediction. Please try again.');
      }
    }
  };

  // Get processing message
  const getProcessingMessage = () => {
    switch (processingPhase) {
      case 'uploading':
        return 'Uploading video to server...';
      case 'processing':
        return 'Processing video and extracting frames...';
      case 'analyzing':
        return 'AI analyzing sign language patterns...';
      case 'completed':
        return prediction ? 'Analysis complete!' : 'Upload complete!';
      default:
        return 'Ready to upload';
    }
  };

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopPolling();
    };
  }, []);

  return (
    <div className="video-upload-container">
      <div className="upload-header">
        <h1>🎯 SilentVoice_BD - Bangla Sign Language Recognition</h1>
        <p>Upload your sign language videos for AI-powered translation</p>
      </div>

      {/* AI Toggle */}
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

      {/* Drag Drop Zone */}
      {!selectedFile && !uploading && !aiProcessing && !prediction && (
        <DragDropZone onFileSelect={handleFileSelect} />
      )}

      {/* Selected File Preview */}
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

      {/* Processing Section */}
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

      {/* Error Message */}
      {error && (
        <div className="error-message">
          ⚠️ {error}
          {(error.includes('timeout') || error.includes('processing') || error.includes('network')) && (
            <button className="retry-btn" onClick={resetUpload}>
              🔄 Try Again
            </button>
          )}
        </div>
      )}

      {/* Success Message */}
      {success && (
        <div className="success-message">
          ✅ {success}
        </div>
      )}

      {/* Prediction Display */}
      {prediction && (
        <PredictionDisplay
          prediction={prediction}
          uploadStatus="completed"
          onReset={resetUpload}
          videoId={videoId}
        />
      )}

      {/* Video List */}
      <VideoList
        refreshTrigger={refreshTrigger}
        onVideoSelect={handleVideoSelect}
      />

      {/* FIXED Video Modal */}
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
              onError={() => console.error('Video playback error')}
            >
              <source 
                src={videoService.getVideoStreamUrl(selectedVideo.id)}
                type={selectedVideo.contentType || 'video/mp4'}
              />
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
