import React, { useState, useRef, useEffect, useCallback } from 'react';
import WebSocketService from '../services/websocketService';
import './LiveWebcamRecognition.css';

const LiveWebcamRecognition = () => {
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const intervalRef = useRef(null);
  const mountedRef = useRef(false);
  const wsInitialized = useRef(false);
  
  const isRecordingRef = useRef(false);
  const sessionIdRef = useRef('');
  const frameSequenceRef = useRef(0); // Track current sequence frames
  
  const [isRecording, setIsRecording] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const [currentPrediction, setCurrentPrediction] = useState('');
  const [confidence, setConfidence] = useState(0);
  const [error, setError] = useState('');
  const [frameCount, setFrameCount] = useState(0);
  const [stream, setStream] = useState(null);
  const [sessionId, setSessionId] = useState('');
  const [framesSent, setFramesSent] = useState(0);
  const [isProcessing, setIsProcessing] = useState(false); // Track AI processing state
  const [sequenceCount, setSequenceCount] = useState(0); // Track completed sequences

  const FRAMES_PER_SEQUENCE = 30; // Match your model training

  useEffect(() => {
    isRecordingRef.current = isRecording;
  }, [isRecording]);

  useEffect(() => {
    sessionIdRef.current = sessionId;
  }, [sessionId]);

  const stopRecording = useCallback(() => {
    console.log('🛑 Stopping recording...');
    setIsRecording(false);
    isRecordingRef.current = false;
    
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
      console.log('⏰ Cleared frame capture interval');
    }
    
    if (stream) {
      stream.getTracks().forEach(track => track.stop());
      setStream(null);
    }
    
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    
    setCurrentPrediction('');
    setConfidence(0);
    setFrameCount(0);
    setSessionId('');
    sessionIdRef.current = '';
    setFramesSent(0);
    setIsProcessing(false);
    setSequenceCount(0);
    frameSequenceRef.current = 0;
  }, [stream]);

  // Fixed frame capture with 30-frame limit
  const captureAndSendFrame = useCallback(() => {
    console.log(`🎯 Frame capture attempt - Sequence frame: ${frameSequenceRef.current}/${FRAMES_PER_SEQUENCE}`);
    
    if (!mountedRef.current || !videoRef.current || !canvasRef.current || 
        !isRecordingRef.current || !sessionIdRef.current || isProcessing) {
      console.log('❌ Skipping frame - requirements not met or processing');
      return;
    }

    // Check if we've completed a 30-frame sequence
    if (frameSequenceRef.current >= FRAMES_PER_SEQUENCE) {
      console.log('✅ Sequence complete! Waiting for AI processing...');
      return; // Don't capture more frames until current sequence is processed
    }

    const video = videoRef.current;
    const canvas = canvasRef.current;
    
    if (video.readyState < 2 || video.videoWidth === 0) {
      console.log('⏳ Video not ready, skipping frame');
      return;
    }

    try {
      const ctx = canvas.getContext('2d');
      canvas.width = video.videoWidth || 640;
      canvas.height = video.videoHeight || 480;
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

      const frameData = canvas.toDataURL('image/jpeg', 0.7);
      WebSocketService.sendFrame(frameData);
      
      // Increment sequence frame counter
      frameSequenceRef.current += 1;
      
      setFramesSent(prev => {
        const newCount = prev + 1;
        console.log(`📤 Frame ${frameSequenceRef.current}/${FRAMES_PER_SEQUENCE} sent (Total: ${newCount})`);
        return newCount;
      });

      // Check if sequence is complete
      if (frameSequenceRef.current >= FRAMES_PER_SEQUENCE) {
        console.log('🎬 30-frame sequence complete! Starting AI processing...');
        setIsProcessing(true);
        setFrameCount(FRAMES_PER_SEQUENCE);
        
        // Send sequence completion signal to backend
        WebSocketService.completeSequence(sessionIdRef.current);
      }

    } catch (err) {
      console.error('❌ Failed to capture/send frame:', err);
    }
  }, [isProcessing]);

  // Reset for next sequence after getting prediction
  const resetForNextSequence = useCallback(() => {
    console.log('🔄 Resetting for next 30-frame sequence...');
    frameSequenceRef.current = 0;
    setFrameCount(0);
    setIsProcessing(false);
    setSequenceCount(prev => prev + 1);
    
    // Brief pause before starting next sequence
    setTimeout(() => {
      console.log('▶️ Ready for next sequence');
    }, 1000);
  }, []);

  // Initialize WebSocket
  useEffect(() => {
    mountedRef.current = true;
    
    if (wsInitialized.current) return;
    wsInitialized.current = true;
    
    const initializeWebSocket = async () => {
      try {
        console.log('🔌 Initializing WebSocket connection...');
        await WebSocketService.connect();
        
        if (mountedRef.current) {
          setIsConnected(true);
          setError('');
          console.log('✅ WebSocket connected and ready');
        }
      } catch (err) {
        console.error('❌ WebSocket initialization failed:', err);
        if (mountedRef.current) {
          setError('Failed to connect to server: ' + err.message);
        }
        wsInitialized.current = false;
      }
    };

    initializeWebSocket();

    return () => {
      mountedRef.current = false;
      setTimeout(() => {
        if (!mountedRef.current) {
          stopRecording();
          if (wsInitialized.current) {
            WebSocketService.disconnect();
            wsInitialized.current = false;
          }
        }
      }, 100);
    };
  }, [stopRecording]);

  const startRecording = async () => {
    let mediaStream;

    try {
      setError('');
      console.log('🎬 Starting 30-frame sequence recording...');
      
      if (!isConnected) {
        throw new Error('WebSocket not connected. Please refresh the page.');
      }

      // Get camera access
      try {
        mediaStream = await navigator.mediaDevices.getUserMedia({
          video: { 
            width: { ideal: 640 },
            height: { ideal: 480 },
            frameRate: { ideal: 15 }
          },
          audio: false
        });
      } catch (mediaErr) {
        throw new Error(`Camera access failed: ${mediaErr.message}`);
      }

      setStream(mediaStream);
      if (videoRef.current) {
        videoRef.current.srcObject = mediaStream;
        
        videoRef.current.onloadedmetadata = () => {
          console.log('📹 Video loaded:', videoRef.current.videoWidth, 'x', videoRef.current.videoHeight);
        };
      }
      console.log('📹 Camera stream started');
      
      // Start live session
      const sessionResponse = await WebSocketService.startLiveSession('current-user');
      console.log('✅ Live session started:', sessionResponse);
      
      setSessionId(sessionResponse.sessionId);
      sessionIdRef.current = sessionResponse.sessionId;
      
      // Subscribe to predictions
      WebSocketService.subscribeToPredictions((prediction) => {
        console.log('🔮 Prediction received:', prediction);
        if (mountedRef.current && !prediction.error) {
          setCurrentPrediction(prediction.prediction);
          setConfidence(prediction.confidence);
          
          // Reset for next sequence after getting prediction
          setTimeout(() => {
            resetForNextSequence();
          }, 2000); // Show result for 2 seconds
        } else if (prediction.error) {
          setError(prediction.message || 'Prediction error');
          setIsProcessing(false);
        }
      });

      // Subscribe to errors
      WebSocketService.subscribeToErrors((error) => {
        console.error('🚨 WebSocket error:', error);
        if (mountedRef.current) {
          setError(error.error || error.message);
          setIsProcessing(false);
        }
      });
      
      setIsRecording(true);
      isRecordingRef.current = true;
      
      // Reset counters
      frameSequenceRef.current = 0;
      setFramesSent(0);
      setSequenceCount(0);
      
      // Start frame capture interval
      console.log('⏰ Starting 30-frame sequence capture...');
      intervalRef.current = setInterval(() => {
        captureAndSendFrame();
      }, 200); // 5fps for better performance

    } catch (err) {
      console.error('❌ Failed to start recording:', err);
      setError(`Failed to start recording: ${err.message}`);
      
      if (mediaStream) {
        mediaStream.getTracks().forEach(track => track.stop());
        setStream(null);
      }
    }
  };

  const getStatusMessage = () => {
    if (!isRecording) return 'Click "Start" to begin recognition';
    if (isProcessing) return 'Processing 30-frame sequence...';
    return `Capturing frame ${frameSequenceRef.current}/${FRAMES_PER_SEQUENCE}`;
  };

  const getConfidenceColor = (conf) => {
    if (conf >= 0.8) return '#4CAF50';
    if (conf >= 0.6) return '#FF9800';
    return '#F44336';
  };

  return (
    <div className="live-webcam-container">
      <div className="webcam-header">
        <h2>🤟 Real-Time Sign Language Recognition</h2>
        <div className="connection-status">
          Status: <span className={isConnected ? 'connected' : 'disconnected'}>
            {isConnected ? 'Connected' : 'Disconnected'}
          </span>
          {sessionId && (
            <div className="session-info">
              Session: {sessionId.substring(0, 8)}...
            </div>
          )}
        </div>
      </div>

      <div className="webcam-content">
        <div className="video-container">
          <video
            ref={videoRef}
            autoPlay
            playsInline
            muted
            className="webcam-video"
          />
          <canvas ref={canvasRef} style={{ display: 'none' }} />
          
          <div className="video-overlay">
            {isRecording && (
              <div className={`recording-indicator ${isProcessing ? 'processing' : ''}`}>
                {isProcessing ? '🔄 AI PROCESSING' : '🔴 RECORDING'}
              </div>
            )}
          </div>
        </div>

        <div className="prediction-panel">
          <div className="prediction-display">
            <h3>Recognition Result:</h3>
            <div className="prediction-text">
              {currentPrediction || getStatusMessage()}
            </div>
            
            {confidence > 0 && (
              <div className="confidence-display">
                <span>Confidence: </span>
                <span 
                  className="confidence-value"
                  style={{ color: getConfidenceColor(confidence) }}
                >
                  {(confidence * 100).toFixed(1)}%
                </span>
              </div>
            )}
            
            <div className="sequence-info">
              <div>Current Sequence: {frameSequenceRef.current}/{FRAMES_PER_SEQUENCE} frames</div>
              <div>Completed Sequences: {sequenceCount}</div>
            </div>
          </div>

          {error && (
            <div className="error-message">
              ⚠️ {error}
            </div>
          )}
        </div>
      </div>

      <div className="control-buttons">
        {!isRecording ? (
          <button 
            onClick={startRecording}
            disabled={!isConnected}
            className="start-button"
          >
            📹 Start Sign Recognition
          </button>
        ) : (
          <button 
            onClick={stopRecording}
            className="stop-button"
          >
            ⏹️ Stop Recognition
          </button>
        )}
      </div>

      <div className="instructions">
        <h4>How it works:</h4>
        <ul>
          <li>🎯 **Perform one sign gesture at a time**</li>
          <li>📹 **System captures exactly 30 frames per gesture**</li>
          <li>🔄 **AI processes each 30-frame sequence**</li>
          <li>📝 **Results appear after each gesture**</li>
          <li>🔁 **System resets for next gesture automatically**</li>
        </ul>
      </div>
    </div>
  );
};

export default LiveWebcamRecognition;
