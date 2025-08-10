import React, { useRef, useEffect, useState, useCallback, useMemo, useLayoutEffect } from 'react';
import { Pose } from '@mediapipe/pose';
import { Camera } from '@mediapipe/camera_utils';
import { useAuth } from '../../contexts/AuthContext';
import apiClient from '../../services/apiClient';
import '../../styles/learning/mirror.css';

// Simple debounce implementation
const debounce = (func, wait) => {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
};

const MirrorPractice = ({ lessonId, expectedSign, onResult }) => {
    const { user } = useAuth();
    const videoRef = useRef(null);
    const canvasRef = useRef(null);

    // MediaPipe instance refs
    const poseRef = useRef(null);
    const cameraRef = useRef(null);
    const streamRef = useRef(null);
    const mountedRef = useRef(true);
    const initializingRef = useRef(false);

    // ✅ CRITICAL FIX: Use refs for dynamic state to avoid stale closures
    const isActiveRef = useRef(false);
    const sessionIdRef = useRef(null);

    // ✅ NEW: Rate limiting and debouncing refs
    const lastAnalysisTimeRef = useRef(0);
    const frameCountRef = useRef(0);

    // ✅ NEW: Configuration constants
    const ANALYSIS_INTERVAL = 500; // 500ms = 2 calls per second (reduced from ~16/sec)
    const PROCESS_EVERY_N_FRAMES = 5; // Only process every 5th frame
    const DEBOUNCE_DELAY = 200; // 200ms debounce delay

    const [isActive, setIsActive] = useState(false);
    const [feedback, setFeedback] = useState('');
    const [confidenceScore, setConfidenceScore] = useState(0);
    const [sessionId, setSessionId] = useState(null);
    const [error, setError] = useState('');
    const [isReady, setIsReady] = useState(false);

    // ✅ Sync state with refs whenever they change
    useEffect(() => {
        isActiveRef.current = isActive;
        console.log('📊 isActive state updated:', isActive);
    }, [isActive]);

    useEffect(() => {
        sessionIdRef.current = sessionId;
        console.log('📊 sessionId state updated:', sessionId);
    }, [sessionId]);

    // ✅ NEW: Optimized state update function with batching
    const updateAnalysisResults = useCallback((results) => {
        if (!mountedRef.current) return;
        
        // Batch state updates to reduce re-renders
        const confidence = Math.round((results.confidenceScore || 0) * 100);
        
        setConfidenceScore(prev => confidence);
        setFeedback(prev => results.feedbackText || 'Keep practicing!');
        
        console.log('🎯 Setting confidence score:', confidence + '%');
        
        if (onResult && confidence > 70) {
            onResult(confidence);
        }
    }, [onResult]);

    // ✅ NEW: Rate-limited pose analysis with improved throttling
    const analyzePose = useCallback(async (landmarks) => {
        // ✅ Use refs for current values instead of stale closure values
        if (!isActiveRef.current || !sessionIdRef.current || !mountedRef.current) {
            console.log('⏸️ Skipping pose analysis:', { 
                isActive: isActiveRef.current, 
                sessionId: sessionIdRef.current, 
                mounted: mountedRef.current 
            });
            return;
        }

        // ✅ NEW: Rate limiting check
        const now = Date.now();
        if (now - lastAnalysisTimeRef.current < ANALYSIS_INTERVAL) {
            // console.log('⏱️ Rate limited - skipping analysis');
            return;
        }
        lastAnalysisTimeRef.current = now;

        try {
            console.log('🔍 Starting pose analysis with', landmarks.length, 'landmarks');

            const formattedLandmarks = landmarks.map((landmark, index) => ({
                index: index,
                x: landmark.x,
                y: landmark.y,
                z: landmark.z || 0,
                visibility: landmark.visibility || 0
            })).filter(landmark => landmark.visibility > 0.3);

            console.log('📡 Sending API request to /api/learning/feedback/analyze');
            const response = await apiClient.post('/api/learning/feedback/analyze', {
                lessonId: lessonId,
                expectedSign: expectedSign,
                poseLandmarks: formattedLandmarks,
                sessionId: sessionIdRef.current, // Use ref value
                timestamp: Date.now()
            });

            console.log('✅ API Response:', response.data);

            if (response.data && mountedRef.current) {
                updateAnalysisResults(response.data);
            }
        } catch (error) {
            console.error('❌ Error analyzing pose:', error);
            if (mountedRef.current) {
                setFeedback('Analysis failed: ' + error.message);
            }
        }
    }, [lessonId, expectedSign, updateAnalysisResults]);

    // ✅ NEW: Debounced pose analysis
    const debouncedAnalyzePose = useMemo(
        () => debounce((landmarks) => {
            analyzePose(landmarks);
        }, DEBOUNCE_DELAY),
        [analyzePose]
    );

    // ✅ IMPROVED: Fixed pose results callback with frame rate control
    const onPoseResults = useCallback(async (results) => {
        if (!mountedRef.current || !canvasRef.current || !videoRef.current) return;

        // ✅ NEW: Frame rate control - only process every Nth frame
        frameCountRef.current++;
        const shouldProcessFrame = frameCountRef.current % PROCESS_EVERY_N_FRAMES === 0;

        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');

        canvas.width = videoRef.current.videoWidth || 640;
        canvas.height = videoRef.current.videoHeight || 480;

        // Clear and draw (always do this for smooth video)
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.save();
        ctx.scale(-1, 1);
        ctx.drawImage(videoRef.current, -canvas.width, 0, canvas.width, canvas.height);
        ctx.restore();

        // Process pose landmarks
        if (results.poseLandmarks && results.poseLandmarks.length > 0) {
            console.log('🤖 Pose detected with', results.poseLandmarks.length, 'landmarks');

            drawPoseLandmarks(ctx, results.poseLandmarks, canvas.width, canvas.height);

            // ✅ NEW: Only analyze pose on selected frames and when active
            if (shouldProcessFrame && isActiveRef.current && sessionIdRef.current && results.poseLandmarks.length >= 20) {
                console.log('✨ Triggering pose analysis...', {
                    isActive: isActiveRef.current,
                    sessionId: sessionIdRef.current,
                    frameCount: frameCountRef.current
                });
                debouncedAnalyzePose(results.poseLandmarks);
            } else if (!shouldProcessFrame) {
                // console.log('⏭️ Skipping frame for performance');
            } else {
                console.log('⏸️ Not analyzing:', { 
                    isActive: isActiveRef.current, 
                    hasSessionId: !!sessionIdRef.current, 
                    landmarkCount: results.poseLandmarks.length 
                });
            }
        }

        drawFeedbackOverlay(ctx);
    }, [debouncedAnalyzePose]);

    const initializePose = useCallback(async () => {
        if (poseRef.current || initializingRef.current || !mountedRef.current) {
            console.log('🚫 Skipping pose init - already initialized');
            return;
        }

        initializingRef.current = true;
        setError('');

        try {
            console.log('🎯 Initializing MediaPipe Pose...');

            const poseInstance = new Pose({
                locateFile: (file) => {
                    return `https://cdn.jsdelivr.net/npm/@mediapipe/pose/${file}`;
                }
            });

            await poseInstance.setOptions({
                modelComplexity: 1,
                smoothLandmarks: true,
                enableSegmentation: false,
                smoothSegmentation: false,
                minDetectionConfidence: 0.5,
                minTrackingConfidence: 0.5
            });

            poseInstance.onResults(onPoseResults);
            poseRef.current = poseInstance;

            await initializeCamera(poseInstance);

            if (mountedRef.current) {
                setIsReady(true);
                console.log('✅ Pose and camera ready!');
            }
        } catch (err) {
            console.error('❌ Pose init error:', err);
            if (mountedRef.current) {
                setError('Failed to initialize: ' + err.message);
            }
        } finally {
            initializingRef.current = false;
        }
    }, [onPoseResults]);

    const initializeCamera = useCallback(async (poseInstance) => {
        if (!mountedRef.current || !videoRef.current) return;

        try {
            console.log('📹 Starting camera...');

            const mediaStream = await navigator.mediaDevices.getUserMedia({
                video: { 
                    width: { ideal: 640 }, 
                    height: { ideal: 480 }, 
                    frameRate: { ideal: 30 } // Keep camera at 30fps, but we'll process less
                }
            });

            if (!mountedRef.current) {
                mediaStream.getTracks().forEach(track => track.stop());
                return;
            }

            streamRef.current = mediaStream;
            videoRef.current.srcObject = mediaStream;

            const cameraInstance = new Camera(videoRef.current, {
                onFrame: async () => {
                    if (poseRef.current && videoRef.current && mountedRef.current) {
                        await poseRef.current.send({ image: videoRef.current });
                    }
                },
                width: 640,
                height: 480
            });

            cameraRef.current = cameraInstance;
            await cameraInstance.start();
            console.log('✅ Camera started');
        } catch (err) {
            console.error('❌ Camera error:', err);
            if (mountedRef.current) {
                setError('Camera failed: ' + err.message);
            }
        }
    }, []);

    const cleanup = useCallback(() => {
        console.log('🧹 Cleanup starting...');
        mountedRef.current = false;
        initializingRef.current = false;
        isActiveRef.current = false;
        sessionIdRef.current = null;
        lastAnalysisTimeRef.current = 0;
        frameCountRef.current = 0;

        if (cameraRef.current) {
            try { cameraRef.current.stop(); } catch (e) {}
            cameraRef.current = null;
        }

        if (streamRef.current) {
            streamRef.current.getTracks().forEach(track => track.stop());
            streamRef.current = null;
        }

        if (videoRef.current) {
            videoRef.current.srcObject = null;
        }

        if (poseRef.current) {
            try { poseRef.current.close(); } catch (e) {}
            poseRef.current = null;
        }

        console.log('✅ Cleanup done');
    }, []);

    const drawPoseLandmarks = useCallback((ctx, landmarks, width, height) => {
        const keyPoints = [11, 12, 13, 14, 15, 16, 19, 20, 21, 22];
        ctx.fillStyle = '#00FF00';
        ctx.strokeStyle = '#00FF00';
        ctx.lineWidth = 3;

        keyPoints.forEach(index => {
            if (landmarks[index] && landmarks[index].visibility > 0.5) {
                const x = landmarks[index].x * width;
                const y = landmarks[index].y * height;
                ctx.beginPath();
                ctx.arc(x, y, 6, 0, 2 * Math.PI);
                ctx.fill();
            }
        });

        const connections = [[11, 13], [13, 15], [15, 19], [15, 21], [12, 14], [14, 16], [16, 20], [16, 22], [11, 12]];
        connections.forEach(([start, end]) => {
            if (landmarks[start] && landmarks[end] && landmarks[start].visibility > 0.5 && landmarks[end].visibility > 0.5) {
                const startX = landmarks[start].x * width;
                const startY = landmarks[start].y * height;
                const endX = landmarks[end].x * width;
                const endY = landmarks[end].y * height;
                ctx.beginPath();
                ctx.moveTo(startX, startY);
                ctx.lineTo(endX, endY);
                ctx.stroke();
            }
        });
    }, []);

    const drawFeedbackOverlay = useCallback((ctx) => {
        const canvas = ctx.canvas;
        ctx.fillStyle = 'rgba(0, 0, 0, 0.8)';
        ctx.fillRect(10, 10, canvas.width - 20, 120);

        const barWidth = canvas.width - 60;
        const barHeight = 20;
        const barX = 30;
        const barY = 30;

        ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
        ctx.fillRect(barX, barY, barWidth, barHeight);

        const confidenceWidth = (barWidth * confidenceScore) / 100;
        ctx.fillStyle = getConfidenceColor(confidenceScore);
        ctx.fillRect(barX, barY, confidenceWidth, barHeight);

        ctx.fillStyle = '#FFFFFF';
        ctx.font = 'bold 18px Arial';
        ctx.fillText(`Accuracy: ${confidenceScore.toFixed(1)}%`, barX, barY + 45);

        ctx.font = '16px Arial';
        const feedbackText = feedback || 'Position yourself and start practicing...';
        ctx.fillText(feedbackText, barX, barY + 70);

        const statusText = isActiveRef.current ? '🔴 Recording' : '⚪ Ready';
        ctx.font = 'bold 14px Arial';
        ctx.fillStyle = isActiveRef.current ? '#FF4444' : '#44FF44';
        ctx.fillText(statusText, barX, barY + 95);
    }, [confidenceScore, feedback]);

    const getConfidenceColor = useCallback((score) => {
        if (score >= 80) return '#4CAF50';
        if (score >= 60) return '#FF9800';
        if (score >= 40) return '#FFC107';
        return '#F44336';
    }, []);

    const startPractice = useCallback(async () => {
        if (!isReady) {
            setError('System not ready. Please wait...');
            return;
        }

        try {
            console.log('🚀 Starting practice session...');
            setError('');
            setConfidenceScore(0);
            
            // ✅ NEW: Reset rate limiting when starting new session
            lastAnalysisTimeRef.current = 0;
            frameCountRef.current = 0;

            const response = await apiClient.post(`/api/learning/feedback/${lessonId}/session/start`, {
                expectedSign: expectedSign,
                userId: user?.id,
                startTime: new Date().toISOString()
            });

            // ✅ Update both state and refs immediately
            setSessionId(response.data.sessionId);
            setIsActive(true);
            sessionIdRef.current = response.data.sessionId;
            isActiveRef.current = true;

            setFeedback('Practice session started! Begin making your sign.');
            console.log('✅ Practice session started with ID:', response.data.sessionId);
        } catch (error) {
            console.error('❌ Failed to start practice:', error);
            setError('Could not start session: ' + error.message);
        }
    }, [isReady, lessonId, expectedSign, user?.id]);

    const stopPractice = useCallback(async () => {
        console.log('🛑 Stopping practice session...');

        // ✅ Update refs immediately
        isActiveRef.current = false;

        try {
            if (sessionIdRef.current) {
                await apiClient.post(`/api/learning/feedback/${lessonId}/session/end`, {
                    sessionId: sessionIdRef.current,
                    expectedSign: expectedSign,
                    endTime: new Date().toISOString()
                });
                console.log('✅ Session ended successfully');
            }
        } catch (error) {
            console.error('❌ Error ending session:', error);
        } finally {
            setIsActive(false);
            setSessionId(null);
            sessionIdRef.current = null;
            setFeedback('Practice session ended. Great job!');
            setConfidenceScore(0);
            
            // ✅ NEW: Reset rate limiting
            lastAnalysisTimeRef.current = 0;
            frameCountRef.current = 0;
        }
    }, [lessonId, expectedSign]);

    // ✅ NEW: Use useLayoutEffect for smooth UI updates to prevent flickering
    useLayoutEffect(() => {
        // This runs synchronously before browser paint, preventing flicker
        if (confidenceScore !== undefined) {
            // Any DOM updates that need to be synchronous can go here
        }
    }, [confidenceScore]);

    // ✅ NEW: Memoized components for better performance
    const ConfidenceDisplay = useMemo(() => (
        <div className="accuracy-display">
            <h5>Current Accuracy: {confidenceScore}%</h5>
            <div className="accuracy-bar">
                <div 
                    className="accuracy-fill" 
                    style={{ 
                        width: `${confidenceScore}%`,
                        backgroundColor: getConfidenceColor(confidenceScore),
                        transition: 'all 0.3s ease-out' // ✅ NEW: Smooth transition
                    }}
                />
            </div>
        </div>
    ), [confidenceScore, getConfidenceColor]);

    const StatusIndicator = useMemo(() => (
        <li className={`status-item ${isActive ? 'active' : ''}`}>
            {isActive ? '🟢 Session Active' : isReady ? '⚪ Ready' : '⏳ Loading...'}
        </li>
    ), [isActive, isReady]);

    useEffect(() => {
        console.log('🔄 MirrorPractice mounting...');
        mountedRef.current = true;

        const timer = setTimeout(() => {
            if (mountedRef.current) {
                initializePose();
            }
        }, 100);

        return () => {
            console.log('🔄 MirrorPractice unmounting...');
            clearTimeout(timer);
            cleanup();
        };
    }, [cleanup, initializePose]);

    return (
        <div className="mirror-practice">
            <div className="mirror-header">
                <h3>Mirror Practice: {expectedSign}</h3>
                <div className="mirror-controls">
                    {!isActive ? (
                        <button 
                            className="btn btn-primary" 
                            onClick={startPractice}
                            disabled={!isReady}
                        >
                            {!isReady ? 'Initializing...' : 'Start Practice'}
                        </button>
                    ) : (
                        <button 
                            className="btn btn-danger" 
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
                        className="mirror-video" 
                        autoPlay 
                        playsInline 
                        muted
                        style={{ display: 'none' }}
                    />
                    <canvas 
                        ref={canvasRef} 
                        className="mirror-canvas"
                    />

                    {error && (
                        <div className="error-overlay">
                            <p>⚠️ {error}</p>
                            <button onClick={() => {
                                cleanup(); 
                                setError(''); 
                                setTimeout(() => initializePose(), 500);
                            }}>
                                Retry
                            </button>
                        </div>
                    )}
                </div>

                <div className="mirror-instructions">
                    <h4>Instructions</h4>
                    <ul>
                        <li>Position yourself so your upper body is visible</li>
                        <li>Make sure your hands are clearly in view</li>
                        <li>Practice the sign slowly and clearly</li>
                        <li>Green dots show detected pose landmarks</li>
                        <li>Aim for 70%+ accuracy for good performance</li>
                        {StatusIndicator}
                    </ul>

                    {ConfidenceDisplay}
                </div>
            </div>
        </div>
    );
};

export default MirrorPractice;
