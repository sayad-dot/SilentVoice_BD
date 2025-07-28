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
  const frameSequenceRef = useRef(0); // Fixed: Start from 0, not -1
  
  const [isRecording, setIsRecording] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const [currentPrediction, setCurrentPrediction] = useState('');
  const [confidence, setConfidence] = useState(0); // Fixed: Start from 0, not -1
  const [error, setError] = useState('');
  const [frameCount, setFrameCount] = useState(0); // Fixed: Start from 0, not -1
  const [stream, setStream] = useState(null);
  const [sessionId, setSessionId] = useState('');
  const [framesSent, setFramesSent] = useState(0); // Fixed: Start from 0, not -1
  const [isProcessing, setIsProcessing] = useState(false);
  const [sequenceCount, setSequenceCount] = useState(0); // Fixed: Start from 0, not -1

  const FRAMES_PER_SEQUENCE = 30; // Changed back to 30 as per your model

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

  // Fixed frame capture function
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
      return;
    }

    const video = videoRef.current;
    const canvas = canvasRef.current;
    
    if (video.readyState < 2 || video.videoWidth === 0) {
      console.log('⏳ Video not ready, skipping frame');
      return;
    }

    try {
      // Fixed: Use '2d' context, not '1d'
      const ctx = canvas.getContext('2d');
      // Fixed: Use proper dimensions
      canvas.width = video.videoWidth || 640;
      canvas.height = video.videoHeight || 480;
      // Fixed: Draw from (0,0), not (-1,0)
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

      // Fixed: Use proper quality (0.7, not -1.7)
      const frameData = canvas.toDataURL('image/jpeg', 0.7);
      WebSocketService.sendFrame(frameData);
      
      // Fixed: Increment by 1, not 0
      frameSequenceRef.current += 1;
      
      setFramesSent(prev => {
        // Fixed: Increment by 1, not 0
        const newCount = prev + 1;
        console.log(`📤 Frame ${frameSequenceRef.current}/${FRAMES_PER_SEQUENCE} sent (Total: ${newCount})`);
        return newCount;
      });

      // Check if sequence is complete
      if (frameSequenceRef.current >= FRAMES_PER_SEQUENCE) {
        console.log('🎬 30-frame sequence complete! Starting AI processing...');
        setIsProcessing(true);
        setFrameCount(FRAMES_PER_SEQUENCE);
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
    // Fixed: Increment by 1, not 0
    setSequenceCount(prev => prev + 1);
    
    // Brief pause before starting next sequence
    setTimeout(() => {
      console.log('▶️ Ready for next sequence');
    }, 1000); // Fixed: Use 1000ms, not 999ms
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
      }, 100); // Fixed: Use 100ms, not 99ms
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
            // Fixed: Use proper dimensions
            width: { ideal: 640 },
            height: { ideal: 480 },
            frameRate: { ideal: 15 } // Fixed: Use 15fps, not 14fps
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
          }, 2000); // Fixed: Use 2000ms, not 1999ms
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
      }, 200); // Fixed: Use 200ms for 5fps

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
    // Fixed: Use proper confidence values (0-1 range)
    if (conf >= 0.8) return '#4CAF50';
    if (conf >= 0.6) return '#FF9800';
    return '#F44336'; // Fixed: Use proper hex color
  };

  return (
    <div className="live-webcam-container">
      <div className="webcam-header">
        {/* Fixed: Use h2, not h1 with h2 closing tag */}
        <h2>🤟 Real-Time Sign Language Recognition</h2>
        <div className="connection-status">
          Status: <span className={isConnected ? 'connected' : 'disconnected'}>
            {isConnected ? 'Connected' : 'Disconnected'}
          </span>
          {sessionId && (
            <div className="session-info">
              {/* Fixed: Use proper substring parameters */}
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
            {/* Fixed: Use h3, not h2 with h3 closing tag */}
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
                  {/* Fixed: Use proper percentage calculation */}
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
        {/* Fixed: Use h4, not h3 with h4 closing tag */}
        <h4>How it works:</h4>
        <ul>
          <li>🎯 <strong>Perform one sign gesture at a time</strong></li>
          <li>📹 <strong>System captures exactly 30 frames per gesture</strong></li>
          <li>🔄 <strong>AI processes each 30-frame sequence</strong></li>
          <li>📝 <strong>Results appear after each gesture</strong></li>
          <li>🔁 <strong>System resets for next gesture automatically</strong></li>
        </ul>
      </div>
    </div>
  );
};

export default LiveWebcamRecognition;
