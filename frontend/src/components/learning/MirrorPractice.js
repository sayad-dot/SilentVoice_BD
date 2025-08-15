import React, { useRef, useEffect, useState, useCallback, useMemo } from 'react';
import { Pose } from '@mediapipe/pose';
import { Camera } from '@mediapipe/camera_utils';
import { useAuth } from '../../contexts/AuthContext';
import apiClient from '../../services/apiClient';
import '../../styles/learning/mirror.css';

// Debounce helper
const debounce = (func, wait) => {
  let timeout;
  return function(...args) {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
};

const MirrorPractice = ({ lessonId, expectedSign, onResult }) => {
  const { user } = useAuth();
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const poseRef = useRef(null);
  const cameraRef = useRef(null);
  const mountedRef = useRef(true);
  const isActiveRef = useRef(false);
  const sessionIdRef = useRef(null);
  const lastAnalysisTimeRef = useRef(0);
  const frameCountRef = useRef(0);
  
  const ANALYSIS_INTERVAL = 500;
  const PROCESS_EVERY_N_FRAMES = 5;
  const DEBOUNCE_DELAY = 200;
  
  const [isActive, setIsActive] = useState(false);
  const [feedback, setFeedback] = useState('');
  const [confidenceScore, setConfidenceScore] = useState(0);
  const [sessionId, setSessionId] = useState(null);
  const [error, setError] = useState('');
  const [isReady, setIsReady] = useState(false);
  const [predictedSign, setPredictedSign] = useState(null);
  const [isInitializing, setIsInitializing] = useState(false);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      cleanup();
    };
  }, []);

  useEffect(() => { isActiveRef.current = isActive; }, [isActive]);
  useEffect(() => { sessionIdRef.current = sessionId; }, [sessionId]);

  const cleanup = useCallback(() => {
    if (cameraRef.current) {
      try {
        cameraRef.current.stop();
      } catch (e) {}
      cameraRef.current = null;
    }
    if (poseRef.current) {
      try {
        poseRef.current.close();
      } catch (e) {}
      poseRef.current = null;
    }
  }, []);

  const updateAnalysisResults = useCallback((results) => {
  if (!mountedRef.current) return;
  
  const confidence = Math.round((results.confidence_score || results.confidenceScore || 0));
  const feedbackText = results.feedback_text || results.feedbackText || 'Keep practicing!';
  const predicted = results.predicted_sign || results.predictedSign;
  const modelStatus = results.model_status || 'unknown';
  
  setConfidenceScore(confidence);
  setFeedback(feedbackText);
  setPredictedSign(predicted);
  
  // Better user feedback during sequence building
  if (modelStatus === 'building_sequence') {
    setFeedback('🔄 Building analysis sequence... Keep signing steadily!');
  }
  
  console.log('🎯 Analysis Results:', {
    confidence: confidence + '%',
    predicted,
    expected: expectedSign,
    isCorrect: results.is_correct || results.isCorrect,
    modelStatus
  });
  
  if (onResult && confidence > 70) {
    onResult(confidence);
  }
}, [onResult, expectedSign]);


  const analyzePose = useCallback(async (landmarks) => {
    if (!isActiveRef.current || !sessionIdRef.current || !mountedRef.current) return;
    
    const now = Date.now();
    if (now - lastAnalysisTimeRef.current < ANALYSIS_INTERVAL) return;
    lastAnalysisTimeRef.current = now;

    try {
      const formattedLandmarks = landmarks.map((lm, idx) => ({
        index: idx,
        x: lm.x,
        y: lm.y,
        z: lm.z || 0,
        visibility: lm.visibility || 0
      })).filter(lm => lm.visibility > 0.3);

      console.log('🚀 Sending pose data:', {
        landmarksCount: formattedLandmarks.length,
        expectedSign,
        lessonId
      });

      const response = await apiClient.post('/api/learning/feedback/analyze', {
        lessonId,
        expectedSign,
        poseLandmarks: formattedLandmarks,
        sessionId: sessionIdRef.current,
        timestamp: Date.now(),
      });

      console.log('📥 Received response:', response.data);

      if (response.data && mountedRef.current) {
        updateAnalysisResults(response.data);
      }
    } catch (err) {
      console.error('❌ Error analyzing pose:', err);
      if (mountedRef.current) {
        setFeedback('Analysis failed: ' + err.message);
      }
    }
  }, [lessonId, expectedSign, updateAnalysisResults]);

  const debouncedAnalyzePose = useMemo(() => debounce(analyzePose, DEBOUNCE_DELAY), [analyzePose]);

  const onPoseResults = useCallback((results) => {
    if (!mountedRef.current || !canvasRef.current || !videoRef.current) return;

    frameCountRef.current++;
    const shouldProcessFrame = frameCountRef.current % PROCESS_EVERY_N_FRAMES === 0;

    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    
    // Set canvas size to match video
    canvas.width = videoRef.current.videoWidth || 640;
    canvas.height = videoRef.current.videoHeight || 480;

    // Clear and draw video
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.save();
    ctx.scale(-1, 1); // Mirror the video
    ctx.drawImage(videoRef.current, -canvas.width, 0, canvas.width, canvas.height);
    ctx.restore();

    // Draw pose landmarks if available
    if (results.poseLandmarks && results.poseLandmarks.length > 0) {
      drawPoseLandmarks(ctx, results.poseLandmarks, canvas.width, canvas.height);
      
      // Send for analysis if conditions are met
      if (shouldProcessFrame && isActiveRef.current && sessionIdRef.current) {
        debouncedAnalyzePose(results.poseLandmarks);
      }
    }

    // Draw feedback overlay
    drawFeedbackOverlay(ctx);
  }, [debouncedAnalyzePose]);

  const drawPoseLandmarks = useCallback((ctx, landmarks, width, height) => {
    const keyPoints = [11, 12, 13, 14, 15, 16, 19, 20, 21, 22]; // Upper body points
    ctx.fillStyle = '#00FF00';
    ctx.strokeStyle = '#00FF00';
    ctx.lineWidth = 2;

    // Draw landmarks
    keyPoints.forEach(i => {
      const landmark = landmarks[i];
      if (landmark && landmark.visibility > 0.5) {
        ctx.beginPath();
        ctx.arc(landmark.x * width, landmark.y * height, 4, 0, 2 * Math.PI);
        ctx.fill();
      }
    });

    // Draw connections
    const connections = [
      [11, 13], [13, 15], [15, 19], [15, 21], // Left arm
      [12, 14], [14, 16], [16, 20], [16, 22], // Right arm
      [11, 12] // Shoulders
    ];
    
    connections.forEach(([start, end]) => {
      const startLm = landmarks[start];
      const endLm = landmarks[end];
      if (startLm && endLm && startLm.visibility > 0.5 && endLm.visibility > 0.5) {
        ctx.beginPath();
        ctx.moveTo(startLm.x * width, startLm.y * height);
        ctx.lineTo(endLm.x * width, endLm.y * height);
        ctx.stroke();
      }
    });
  }, []);

  const drawFeedbackOverlay = useCallback((ctx) => {
    const canvas = ctx.canvas;
    
    // Background overlay
    ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
    ctx.fillRect(10, 10, canvas.width - 20, 140);

    const barWidth = canvas.width - 60;
    const barHeight = 20;
    const barX = 30;
    const barY = 30;

    // Progress bar background
    ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
    ctx.fillRect(barX, barY, barWidth, barHeight);

    // Progress bar fill
    const confidenceWidth = (barWidth * confidenceScore) / 100;
    const color = confidenceScore >= 80 ? '#4CAF50' : 
                  confidenceScore >= 60 ? '#FF9800' : 
                  confidenceScore >= 40 ? '#FFC107' : '#F44336';
    ctx.fillStyle = color;
    ctx.fillRect(barX, barY, confidenceWidth, barHeight);

    // Text
    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 18px Arial';
    ctx.fillText(`Accuracy: ${confidenceScore.toFixed(1)}%`, barX, barY + 45);

    ctx.font = '16px Arial';
    const feedbackText = feedback || 'Position yourself and start practicing...';
    ctx.fillText(feedbackText, barX, barY + 70);

    // Predicted sign
    if (predictedSign) {
      ctx.font = '14px Arial';
      ctx.fillStyle = '#90EE90';
      ctx.fillText(`Detected: ${predictedSign}`, barX, barY + 90);
    }

    // Status
    const statusText = isActiveRef.current ? '🔴 Recording' : '⚪ Ready';
    ctx.font = 'bold 14px Arial';
    ctx.fillStyle = isActiveRef.current ? '#FF4444' : '#44FF44';
    ctx.fillText(statusText, barX, barY + 115);
  }, [confidenceScore, feedback, predictedSign]);

  const initializeMediaPipe = useCallback(async () => {
    if (isInitializing || poseRef.current) return;
    
    setIsInitializing(true);
    setError('');

    try {
      console.log('🚀 Initializing MediaPipe...');
      
      // Initialize Pose
      const pose = new Pose({
        locateFile: (file) => `https://cdn.jsdelivr.net/npm/@mediapipe/pose/${file}`
      });

      await pose.setOptions({
        modelComplexity: 1,
        smoothLandmarks: true,
        enableSegmentation: false,
        smoothSegmentation: false,
        minDetectionConfidence: 0.5,
        minTrackingConfidence: 0.5
      });

      pose.onResults(onPoseResults);
      poseRef.current = pose;
      
      console.log('✅ MediaPipe Pose initialized');

      // Initialize Camera
      if (videoRef.current) {
        const camera = new Camera(videoRef.current, {
          onFrame: async () => {
            if (poseRef.current && videoRef.current) {
              await poseRef.current.send({ image: videoRef.current });
            }
          },
          width: 640,
          height: 480
        });
        
        await camera.start();
        cameraRef.current = camera;
        
        console.log('✅ Camera initialized');
        setIsReady(true);
        setFeedback('Camera ready! Click "Start Practice" to begin.');
      }

    } catch (err) {
      console.error('❌ MediaPipe initialization failed:', err);
      setError('Failed to initialize camera: ' + err.message);
    } finally {
      setIsInitializing(false);
    }
  }, [onPoseResults]);

  useEffect(() => {
    initializeMediaPipe();
  }, [initializeMediaPipe]);

  const startPractice = useCallback(async () => {
    if (!isReady) {
      setError('System not ready. Please wait...');
      return;
    }

    try {
      setError('');
      setConfidenceScore(0);
      setPredictedSign(null);
      lastAnalysisTimeRef.current = 0;
      frameCountRef.current = 0;

      const response = await apiClient.post(`/api/learning/feedback/${lessonId}/session/start`, {
        expectedSign,
        userId: user?.id,
        startTime: new Date().toISOString(),
      });

      setSessionId(response.data.sessionId);
      setIsActive(true);
      sessionIdRef.current = response.data.sessionId;
      isActiveRef.current = true;
      setFeedback('Practice session started! Begin making your sign.');
      
      console.log('✅ Practice session started:', response.data.sessionId);
    } catch (err) {
      console.error('❌ Failed to start practice:', err);
      setError('Could not start session: ' + err.message);
    }
  }, [isReady, lessonId, expectedSign, user?.id]);

  const stopPractice = useCallback(async () => {
    isActiveRef.current = false;

    try {
      if (sessionIdRef.current) {
        await apiClient.post(`/api/learning/feedback/${lessonId}/session/end`, {
          sessionId: sessionIdRef.current,
          expectedSign,
          endTime: new Date().toISOString(),
        });
        console.log('✅ Practice session ended');
      }
    } catch (err) {
      console.error('❌ Error ending session:', err);
    } finally {
      setIsActive(false);
      setSessionId(null);
      sessionIdRef.current = null;
      setFeedback('Practice session ended. Great job!');
      setConfidenceScore(0);
      setPredictedSign(null);
      lastAnalysisTimeRef.current = 0;
      frameCountRef.current = 0;
    }
  }, [lessonId, expectedSign]);

  return (
    <div className="mirror-practice">
      <div className="mirror-header">
        <h3>Mirror Practice: {expectedSign}</h3>
        <div className="mirror-controls">
          {!isActive ? (
            <button 
              className="btn btn-primary" 
              onClick={startPractice}
              disabled={!isReady || isInitializing}
            >
              {isInitializing ? 'Initializing...' : isReady ? 'Start Practice' : 'Getting Ready...'}
            </button>
          ) : (
            <button 
              className="btn btn-secondary" 
              onClick={stopPractice}
            >
              Stop Practice
            </button>
          )}
        </div>
      </div>

      <div className="mirror-container">
        <div className="video-container">
          <video 
            ref={videoRef} 
            style={{ display: 'none' }} 
            autoPlay 
            muted 
            playsInline 
            width="640"
            height="480"
          />
          <canvas 
            ref={canvasRef} 
            className="mirror-canvas"
            style={{ 
              width: '100%', 
              maxWidth: '640px', 
              height: 'auto',
              border: '1px solid #ccc',
              borderRadius: '8px'
            }}
          />
        </div>

        <div className="accuracy-display">
          <h5>Recognition Accuracy</h5>
          <div className="accuracy-bar">
            <div 
              className="accuracy-fill" 
              style={{ 
                width: `${confidenceScore}%`,
                backgroundColor: confidenceScore >= 80 ? '#4CAF50' : 
                                confidenceScore >= 60 ? '#FF9800' : 
                                confidenceScore >= 40 ? '#FFC107' : '#F44336'
              }}
            />
          </div>
          <div style={{ textAlign: 'center', marginTop: '8px', fontSize: '14px' }}>
            {confidenceScore.toFixed(1)}%
            {predictedSign && (
              <div style={{ color: '#4CAF50', fontSize: '12px' }}>
                Detected: {predictedSign}
              </div>
            )}
          </div>
        </div>
      </div>

      {error && (
        <div className="mirror-error">
          ⚠️ {error}
        </div>
      )}

      <div className="mirror-instructions">
        <h4>Practice Instructions</h4>
        <ul>
          <li>Position yourself clearly in front of the camera</li>
          <li>Make sure both hands are visible</li>
          <li>Perform the sign slowly and deliberately</li>
          <li>Hold the final position for 2-3 seconds</li>
          <li>Good lighting will improve recognition accuracy</li>
        </ul>
      </div>
    </div>
  );
};

export default MirrorPractice;


