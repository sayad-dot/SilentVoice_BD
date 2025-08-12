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
    const frameSequenceRef = useRef(0);
    
    const [isRecording, setIsRecording] = useState(false);
    const [isConnected, setIsConnected] = useState(false);
    const [currentPrediction, setCurrentPrediction] = useState('');
    const [confidence, setConfidence] = useState(0);
    const [error, setError] = useState('');
    const [frameCount, setFrameCount] = useState(0);
    const [stream, setStream] = useState(null);
    const [sessionId, setSessionId] = useState('');
    const [framesSent, setFramesSent] = useState(0);
    const [isProcessing, setIsProcessing] = useState(false);
    const [sequenceCount, setSequenceCount] = useState(0);
    const [statusMessage, setStatusMessage] = useState('Click "Start" to begin recognition');
    
    const FRAMES_PER_SEQUENCE = 30;
    
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
        setStatusMessage('Click "Start" to begin recognition');
    }, [stream]);
    
    const captureAndSendFrame = useCallback(() => {
        if (!mountedRef.current || !videoRef.current || !canvasRef.current || 
            !isRecordingRef.current || !sessionIdRef.current) {
            return;
        }
        
        // FIXED: Don't skip frames if processing, just continue collecting
        if (frameSequenceRef.current >= FRAMES_PER_SEQUENCE) {
            console.log('✅ Sequence complete! Waiting for AI processing...');
            setStatusMessage('Processing 30-frame sequence...');
            return;
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
            
            frameSequenceRef.current += 1;
            setFrameCount(frameSequenceRef.current);
            setFramesSent(prev => prev + 1);
            
            console.log(`📤 Frame ${frameSequenceRef.current}/${FRAMES_PER_SEQUENCE} sent`);
            setStatusMessage(`Capturing frame ${frameSequenceRef.current}/${FRAMES_PER_SEQUENCE}`);
            
            if (frameSequenceRef.current >= FRAMES_PER_SEQUENCE) {
                console.log('🎬 30-frame sequence complete! Starting AI processing...');
                setIsProcessing(true);
                setStatusMessage('Processing 30-frame sequence...');
                
                // FIXED: Don't reset immediately, wait for prediction
            }
            
        } catch (err) {
            console.error('❌ Failed to capture/send frame:', err);
            setError('Failed to capture frame: ' + err.message);
        }
    }, []);
    
    const resetForNextSequence = useCallback(() => {
        console.log('🔄 Resetting for next 30-frame sequence...');
        frameSequenceRef.current = 0;
        setFrameCount(0);
        setIsProcessing(false);
        setSequenceCount(prev => prev + 1);
        setStatusMessage('Ready for next sequence');
        
        // Small delay before starting next sequence
        setTimeout(() => {
            if (isRecordingRef.current) {
                setStatusMessage(`Capturing frame 0/${FRAMES_PER_SEQUENCE}`);
                console.log('▶️ Ready for next sequence');
            }
        }, 2000);
    }, []);
    
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
            
            const sessionResponse = await WebSocketService.startLiveSession('current-user');
            console.log('✅ Live session started:', sessionResponse);
            
            setSessionId(sessionResponse.sessionId);
            sessionIdRef.current = sessionResponse.sessionId;
            
            // FIXED: Subscribe to predictions with better parsing
            WebSocketService.subscribeToPredictions((predictionMessage) => {
                console.log('📨 Raw prediction message received:', predictionMessage);
                
                try {
                    // FIXED: Properly parse the prediction object
                    let prediction;
                    if (typeof predictionMessage === 'string') {
                        prediction = JSON.parse(predictionMessage);
                    } else {
                        prediction = predictionMessage;
                    }
                    
                    console.log('🔮 Parsed prediction:', prediction);
                    
                    if (mountedRef.current) {
                        // FIXED: Handle progress messages vs final predictions
                        if (prediction.progress) {
                            // This is a progress update, just update status
                            setStatusMessage(prediction.message || `Collecting frames... ${prediction.frame_count || 0}/30`);
                            console.log('📊 Progress update:', prediction.message);
                        } else if (!prediction.error) {
                            // This is a final prediction
                            const predictionText = prediction.prediction || prediction.predicted_text || 'Unknown';
                            const confidenceValue = prediction.confidence || prediction.confidenceScore || 0;
                            
                            console.log(`✅ Final prediction: "${predictionText}" with confidence: ${confidenceValue}`);
                            
                            setCurrentPrediction(predictionText);
                            setConfidence(confidenceValue);
                            setStatusMessage(`Predicted: ${predictionText} (${Math.round(confidenceValue * 100)}%)`);
                            
                            // Reset for next sequence after getting prediction
                            setTimeout(() => {
                                resetForNextSequence();
                            }, 3000);
                        } else {
                            console.error('🚨 Prediction error:', prediction);
                            setError(prediction.message || 'Prediction error');
                            setStatusMessage('Prediction failed');
                            setIsProcessing(false);
                        }
                    }
                } catch (parseError) {
                    console.error('❌ Failed to parse prediction message:', parseError);
                    console.error('Raw message was:', predictionMessage);
                    setError('Failed to parse prediction response');
                    setStatusMessage('Parsing error');
                    setIsProcessing(false);
                }
            });
            
            // FIXED: Subscribe to errors with better error handling
            WebSocketService.subscribeToErrors((errorMessage) => {
                console.error('🚨 WebSocket error:', errorMessage);
                if (mountedRef.current) {
                    let errorText = 'Unknown error';
                    try {
                        if (typeof errorMessage === 'string') {
                            const errorObj = JSON.parse(errorMessage);
                            errorText = errorObj.error || errorObj.message || errorMessage;
                        } else {
                            errorText = errorMessage.error || errorMessage.message || 'Unknown error';
                        }
                    } catch (e) {
                        errorText = String(errorMessage);
                    }
                    setError(errorText);
                    setStatusMessage('Error occurred');
                    setIsProcessing(false);
                }
            });
            
            setIsRecording(true);
            isRecordingRef.current = true;
            
            // Reset counters
            frameSequenceRef.current = 0;
            setFramesSent(0);
            setSequenceCount(0);
            setFrameCount(0);
            setCurrentPrediction('');
            setConfidence(0);
            
            // Start frame capture interval
            console.log('⏰ Starting 30-frame sequence capture...');
            setStatusMessage('Starting frame capture...');
            
            intervalRef.current = setInterval(() => {
                captureAndSendFrame();
            }, 200); // 5fps
            
        } catch (err) {
            console.error('❌ Failed to start recording:', err);
            setError(`Failed to start recording: ${err.message}`);
            setStatusMessage('Failed to start');
            if (mediaStream) {
                mediaStream.getTracks().forEach(track => track.stop());
                setStream(null);
            }
        }
    };
    
    const getConfidenceColor = (conf) => {
        if (conf >= 0.8) return '#4CAF50';
        if (conf >= 0.6) return '#FF9800';
        return '#F44336';
    };
    
    return (
        <div className="live-webcam-container">
            {/* Header */}
            <div className="webcam-header">
                <h2>🔴 Real-time Sign Language Recognition</h2>
                <div className={`connection-status ${isConnected ? 'connected' : 'disconnected'}`}>
                    <span className={`status-indicator ${isConnected ? 'connected' : 'disconnected'}`}></span>
                    {isConnected ? 'Connected' : 'Disconnected'}
                </div>
            </div>
            
            {/* Main Content */}
            <div className="webcam-content">
                {/* Video Container */}
                <div className="video-container">
                    <video
                        ref={videoRef}
                        autoPlay
                        muted
                        playsInline
                        className="webcam-video"
                    />
                    <canvas ref={canvasRef} style={{ display: 'none' }} />
                    
                    {isRecording && (
                        <div className="video-overlay">
                            <div className="recording-indicator">
                                🔴 Recording
                            </div>
                        </div>
                    )}
                    
                    {isProcessing && (
                        <div className="loading-overlay">
                            <div className="loading-spinner"></div>
                        </div>
                    )}
                </div>
                
                {/* Prediction Panel */}
                <div className="prediction-panel">
                    <div className="prediction-display">
                        <h3>🤖 AI Prediction</h3>
                        <div className="prediction-text">
                            {currentPrediction || 'Waiting for sign...'}
                        </div>
                        
                        {confidence > 0 && (
                            <div className="confidence-display">
                                <div 
                                    className="confidence-value" 
                                    style={{ color: getConfidenceColor(confidence) }}
                                >
                                    Confidence: {Math.round(confidence * 100)}%
                                </div>
                            </div>
                        )}
                        
                        <div className="frame-info">
                            <div>Status: {statusMessage}</div>
                            <div>Frames: {frameCount}/{FRAMES_PER_SEQUENCE}</div>
                            <div>Sequence: #{sequenceCount}</div>
                        </div>
                        
                        {error && (
                            <div className="error-message">
                                ⚠️ {error}
                            </div>
                        )}
                        
                        <div className="session-info">
                            Session: {sessionId.substring(0, 8)}...
                        </div>
                    </div>
                </div>
            </div>
            
            {/* Controls */}
            <div className="control-buttons">
                {!isRecording ? (
                    <button 
                        onClick={startRecording}
                        disabled={!isConnected}
                        className="start-button"
                    >
                        🎬 Start Recognition
                    </button>
                ) : (
                    <button 
                        onClick={stopRecording}
                        className="stop-button"
                    >
                        🛑 Stop Recognition
                    </button>
                )}
            </div>
            
            {/* Instructions */}
            <div className="instructions">
                <h4>📋 Instructions</h4>
                <ul>
                    <li>Position yourself clearly in front of the camera</li>
                    <li>Ensure good lighting and minimal background clutter</li>
                    <li>Perform signs clearly and hold them briefly</li>
                    <li>Wait for 30 frames to be collected before prediction</li>
                    <li>Each sequence takes about 6 seconds to complete</li>
                </ul>
            </div>
            
            {/* Debug Info */}
            <div className="debug-info">
                <h4>🛠️ Debug Information</h4>
                <div>WebSocket: {isConnected ? '✅ Connected' : '❌ Disconnected'}</div>
                <div>Recording: {isRecording ? '✅ Active' : '⭕ Inactive'}</div>
                <div>Processing: {isProcessing ? '⏳ Processing' : '✅ Ready'}</div>
                <div>Total Frames Sent: {framesSent}</div>
                <div>Current Buffer: {frameCount}/{FRAMES_PER_SEQUENCE}</div>
                <div>Completed Sequences: {sequenceCount}</div>
            </div>
        </div>
    );
};

export default LiveWebcamRecognition;
